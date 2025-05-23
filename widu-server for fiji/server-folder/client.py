#!/usr/bin/python3
import sys
import socket

def send_task(folder_path, host='localhost', port=65432):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.connect((host, port))
        s.sendall(folder_path.encode())
        data = s.recv(1024).decode()
        print(f"Server response: {data}")


def print_help():
    help_message = """
    Usage: python client.py <input_folder>

    Arguments:
    input_folder   Path to the folder containing PNG images to be processed.

    Example:
    python client.py /path/to/input/folder
    """
    print(help_message)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Error: Invalid number of arguments.")
        print_help()
        sys.exit(1)

    input_folder = sys.argv[1]
    send_task(input_folder)
