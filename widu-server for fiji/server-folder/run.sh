#!/bin/bash
eval "$(/home/widu/miniconda3/bin/conda shell.bash hook)"
conda activate widu
LD_LIBRARY_PATH=/home/widu/miniconda3/envs/widu/lib python /home/widu/run.py $1 
