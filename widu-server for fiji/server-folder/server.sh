#!/bin/bash
eval "$(/home/widu/miniconda3/bin/conda shell.bash hook)"
conda activate widu
python /home/widu/server.py
