package com.example.integrationtest

import com.example.lb.LoadBalancerServer
import com.example.server.ServerApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.AfterAll
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.junit.jupiter.api.Assertions.*
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import java.util.concurrent.TimeUnit

class LoadBalancerServerIntegrationTest {

    companion object {
        private const val LB_PORT = 8080
        private val SERVER_PORTS = listOf(8081, 8082, 8083)
        private val restTemplate = TestRestTemplate()

        private lateinit var loadBalancer: ConfigurableApplicationContext
        private val servers = mutableListOf<ConfigurableApplicationContext>()


        @BeforeAll
        @JvmStatic
        fun setup() {
            // Start load balancer
            startLoadBalancer()
            // Wait for load balancer to initialize
            TimeUnit.SECONDS.sleep(10)
            // Start servers
            startServers()
            // Wait for servers to register
            TimeUnit.SECONDS.sleep(5)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            // Shutdown all applications
            servers.forEach { it.close() }
            loadBalancer.close()
        }

        private fun startLoadBalancer() {
            loadBalancer = SpringApplication.run(
                LoadBalancerServer::class.java,
                "--server.port=$LB_PORT",
                "--spring.main.web-application-type=servlet"
            )
        }

        private fun startServers() {
            SERVER_PORTS.forEach { port ->
                val server = SpringApplication.run(
                    ServerApplication::class.java,
                    "--server.port=$port",
                    "--spring.main.web-application-type=servlet"
                )
                servers.add(server)
            }
        }

    }

    @Test
    fun `test load balancer registration and routing`() {
        // Verify all servers are registered
        val serversStatus = restTemplate.getForEntity(
            "http://localhost:$LB_PORT/servers/status",
            Map::class.java
        )
        assertEquals(HttpStatus.OK, serversStatus.statusCode)

        // Make multiple requests and verify load balancing
        val responses = (1..6).map {
            restTemplate.getForEntity(
                "http://localhost:$LB_PORT/api?message=test$it",
                String::class.java
            )
        }

        responses.forEach {
            assertEquals(HttpStatus.OK, it.statusCode)
        }

        // Verify round-robin distribution
        val uniquePorts = responses.map {
            it.body?.substringBetween("port ", " received")?.toInt()
        }.distinct()
        assertEquals(SERVER_PORTS.size, uniquePorts.size)
    }

    @Test
    fun `test server shutdown and reintegration`() {
        // Shut down one of the servers
        val portToShutdown = SERVER_PORTS[0]
        servers.find { it.environment.getProperty("local.server.port") == portToShutdown.toString() }?.close()
        TimeUnit.SECONDS.sleep(10) // Wait for health check update

        // Verify requests are rerouted to remaining active servers
        val activeResponses = (1..6).map {
            restTemplate.getForEntity(
                "http://localhost:$LB_PORT/api?message=active$it",
                String::class.java
            )
        }

        activeResponses.forEach {
            assertEquals(HttpStatus.OK, it.statusCode)
        }

        val activePorts = activeResponses.map {
            it.body?.substringBetween("port ", " received")?.toInt()
        }.distinct()
        assertEquals(SERVER_PORTS.size - 1, activePorts.size)

        // Bring back the shut-down server
        val restartedServer = SpringApplication.run(
            ServerApplication::class.java,
            "--server.port=$portToShutdown",
            "--spring.main.web-application-type=servlet"
        )
        TimeUnit.SECONDS.sleep(10) // Wait for server to re-register

        // Verify requests are distributed across all servers
        val finalResponses = (1..6).map {
            restTemplate.getForEntity(
                "http://localhost:$LB_PORT/api?message=final$it",
                String::class.java
            )
        }

        finalResponses.forEach {
            assertEquals(HttpStatus.OK, it.statusCode)
        }

        val finalPorts = finalResponses.map {
            it.body?.substringBetween("port ", " received")?.toInt()
        }.distinct()
        assertEquals(SERVER_PORTS.size, finalPorts.size)

        // Clean up restarted server
        restartedServer.close()
    }

    private fun String.substringBetween(start: String, end: String): String? {
        val startIndex = this.indexOf(start)
        if (startIndex < 0) return null
        val endIndex = this.indexOf(end, startIndex + start.length)
        if (endIndex < 0) return null
        return this.substring(startIndex + start.length, endIndex)
    }
}