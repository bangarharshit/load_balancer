from flask import Flask, request
import sys
import requests
import argparse

app = Flask(__name__)

def register_server(port):
    try:
        registration_url = "http://localhost:8080/register"
        data = {"port": port}
        response = requests.post(registration_url, json=data)
        print(f"Registration response: {response.text}")
    except requests.exceptions.RequestException as e:
        print(f"Failed to register server: {e}")

@app.route('/')
def home():
    return f'Server on port {app.config["PORT"]} is running!'

@app.route('/api', methods=['GET'])
def api():
    message = request.args.get('message', 'No message provided')
    print(f"Server on port {app.config['PORT']} received: {message}")
    return f"Server on port {app.config['PORT']} received: {message}"


# post api which accepts a request body and print the same
@app.route('/api', methods=['POST'])
def api_post():
    data = request.get_json()
    print(f"Server on port {app.config['PORT']} received: {data}")
    return f"Server on port {app.config['PORT']} received: {data}"

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', type=int, required=True, help='Port number for the server')
    args = parser.parse_args()
    
    app.config['PORT'] = args.port
    register_server(args.port)
    app.run(port=args.port) 