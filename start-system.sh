#!/bin/bash
# Available commands:
#
# Start all services (load balancer + 3 servers):
#   ./start-system.sh
#   ./start-system.sh start
#
# Stop all services:
#   ./start-system.sh stop
#
# Start single server (port 8081 only):
#   ./start-system.sh start-single
#
# Stop single server (port 8081 only):
#   ./start-system.sh stop-single
#
# Show usage help:
#   ./start-system.sh help
#   ./start-system.sh --help
#   ./start-system.sh -
# Available commands:
# Start all services (load balancer + 3 servers):
#   ./start-system.sh
#   ./start-system.sh start
#
# Stop all services:
#   ./start-system.sh stop
#
# Start single server (port 8081 only):
#   ./start-system.sh start-single
#
# Stop single server (port 8081 only):
#   ./start-system.sh stop-single
#
# Show usage help:
#   ./start-system.sh help
#   ./start-system.sh --help
#   ./start-system.sh -           h


# Configuration constants
LOAD_BALANCER_STARTUP_WAIT=10
SERVER_PORTS=(8081 8082 8083)
GRADLE_CMD="./gradlew"

# Print usage instructions
show_usage() {
    echo "Usage: $0 [start|stop]"
    echo "  start - Start the system (default)"
    echo "  stop  - Stop all running services"
}

# Start the load balancer
start_load_balancer() {
    echo "Starting load balancer..."
    $GRADLE_CMD :load-balancer:bootRun &
    echo "Waiting $LOAD_BALANCER_STARTUP_WAIT seconds for load balancer to initialize..."
    sleep $LOAD_BALANCER_STARTUP_WAIT
}

# Start multiple server instances
start_servers() {
    echo "Starting server instances..."
    for port in "${SERVER_PORTS[@]}"; do
        echo "Starting server on port $port"
        $GRADLE_CMD :server:bootRun --args="--server.port=$port" &
    done
}

# Stop all services
stop_services() {
    echo "Stopping all services..."
    pkill -f bootRun
}

case "$1" in
    "stop")
        stop_services
        ;;
    "stop-single")
        echo "Stopping server on port ${SERVER_PORTS[0]}..."
        pkill -f "server.port=${SERVER_PORTS[0]}"
        ;;
    "start"|"")
        start_load_balancer
        start_servers
        ;;
    "start-single")
        echo "Starting single server on port ${SERVER_PORTS[0]}..."
        $GRADLE_CMD :server:bootRun --args="--server.port=${SERVER_PORTS[0]}" &
        ;;
    *)
        show_usage
        exit 1
        ;;
esac