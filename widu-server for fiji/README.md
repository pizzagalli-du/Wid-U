# Installation instructions for Wid-U server for FIJI plugin

These instructions were tested on CentOS 7 with Nvidia GPU card. The FIJI plugin splits the images in blocks 224x244 pixels and sends them via SSH to a folder and launches the segmentation command. Segmentation creates a 'results' folder containing all result block. When done, a 'done.txt' file is created. FIJI plugin sees that file, retrieves and assembles blocks to the pseudofluorescence image.

1. Set up a user called 'widu'
1. In its home, install miniforge conda distribution
1. Copy all files in 'server-folder' to widu home
1. Copy models in a `models/` folder inside widu home
1. Import 'conda-environment.yml' to install all needed 
1. To autostart (via SystemD), copy widu.server in `lib/systemd/system`
1. Run `sudo systemctl daemon-reload`
1. To enable task `sudo systemctl enable widu.service`
1. To start `sudo systemctl start widu.service`
1. You can check status with `sudo systemctl status widu.service`

Adjust settings in `run.py` script to match your configuration. In particular the settings below

    max_gpu_ram=3072 # TF GPU RAM in MBs
    max_cpu_threads=4 # TF threads
    max_batch=256 # files are processed in batches to avoid filling CPU RAM in MBs
    tf_batch=4 # TF prediction batch


In FIJI plugin:

1. Run Edit > Setting > Wid-U Plugin:

   - Host: address or IP of wid-u server (can also be `localhost`)
   - Port: port of wid-u server (usually `22`)
   - Username: `widu`
   - Cache folder: where temporary files should be saved. They are removed after each run, but the folder should be big enough to contain your biggest dataset. Usually `/tmp`
   - Command: `/path/to/widu/client.py`. This will run `client.py` and queue the task on server. Each plugin run will add a task to a FIFO queue
   - Authentication method. You can set a key in widu home and choose `RSA`. WHen clicking OK, you'll be asked to provide the location of corresponding public key. A connection test will also be performed.

2. Open a timelapse and run Analyze > Wid-U
