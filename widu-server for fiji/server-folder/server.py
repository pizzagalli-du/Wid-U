import os
import subprocess
import socket
import threading
from queue import Queue

# Queue to hold tasks
task_queue = Queue()

# Function to process tasks
def process_task(input_folder):
    subprocess.call(['/home/widu/run.sh', input_folder])

# Function to handle client connections
def handle_client(conn, addr):
    print(f"Connected by {addr}")
    while True:
        data = conn.recv(1024).decode()
        if not data:
            break
        print(f"Received folder path: {data}")
        task_queue.put(data)
        conn.sendall(b"Task received")
    conn.close()

# Function to process tasks from the queue
def process_tasks():
    while True:
        folder_path = task_queue.get()
        if folder_path is None:
            break
        process_task(folder_path)
        task_queue.task_done()

def start_server(host='localhost', port=65432):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind((host, port))
        s.listen()
        print(f"Server listening on {host}:{port}")

        # Start a thread to process tasks
        threading.Thread(target=process_tasks, daemon=True).start()

        while True:
            conn, addr = s.accept()
            threading.Thread(target=handle_client, args=(conn, addr), daemon=True).start()

if __name__ == "__main__":
    start_server()
