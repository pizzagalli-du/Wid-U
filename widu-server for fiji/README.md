# Installation instructions for Wid-U server for FIJI plugin

These instructions were tested on CentOS 7 with Nvidia GPU card. The FIJI plugin splits the images in blocks 224x244 pixels and sends them via SSH to a folder and launches the segmentation command. Segmentation creates a 'results' folder containing all result block. When done, a 'done.txt' file is created. FIJI plugin sees that file, retrieves and assembles blocks to the pseudofluorescence image.

1. Set up a user called 'widu'
1. In its home, install miniforge conda distribution
1. Copy all files in 'server-folder' to widu home
1. Import 'conda-environment.yml' to install all needed 
1. To autostart (via SystemD), copy widu.server in `lib/systemd/system`
1. Run `sudo systemcl daemon-reload`
1. To enable task `sudo systemctl enable widu.service`
1. To start `sudo systemctl start widu.service`
1. You can check status with `sudo systemctl status widu.service`

Adjust settings in `run.py` script to match your configuration. In particular the settings below

    max_gpu_ram=3072 # TF GPU RAM
    max_cpu_threads=4 # TF threads
    max_batch=256 # files are processed in batches to avoid filling CPU RAM
    tf_batch=4 # TF prediction batch


