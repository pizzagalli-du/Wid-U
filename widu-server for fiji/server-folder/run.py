import sys, os
import tensorflow as tf
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing import image
import numpy as np

def save_images(images, filenames, output_folder):
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)
    for img_array, filename in zip(images, filenames):
        img = image.array_to_img(img_array)
        img.save(os.path.join(output_folder, filename))

def main(input_folder, model_path):
    # Adjust these
    max_gpu_ram=3072 # TF GPU RAM
    max_cpu_threads=4 # TF threads
    max_batch=256 # files are processed in batches to avoid filling CPU RAM
    tf_batch=4 # TF prediction batch

    gpus = tf.config.experimental.list_physical_devices('GPU')
    if gpus:
        tf.config.experimental.set_virtual_device_configuration(gpus[0],
        [tf.config.experimental.VirtualDeviceConfiguration(memory_limit=max_gpu_ram)])

    tf.config.threading.set_intra_op_parallelism_threads(max_cpu_threads)
    tf.config.threading.set_inter_op_parallelism_threads(max_cpu_threads)

    # Create output folder
    output_folder = os.path.join(input_folder, "results")
    os.makedirs(os.path.dirname(output_folder), exist_ok=True)

    # Load the model
    model = load_model(model_path)

    filenames = []
    for filename in os.listdir(input_folder):
        if filename.lower().endswith(".png"):
            filenames.append(filename)

    for i in range(0, len(filenames), max_batch):
        fbatch=filenames[i:i+max_batch]
        images=[]
        for f in fbatch:
            img_path = os.path.join(input_folder, f)
            img = image.load_img(img_path, target_size=(224, 224),  color_mode = "grayscale")
            img_array = image.img_to_array(img)
            images.append(img_array)
        
        images = np.array(images) / 255.0
        # TF
        predictions = model.predict(images, batch_size=tf_batch)
        save_images(predictions, fbatch, output_folder)


    # Send message that task is done and results can be retrieved
    with open(os.path.join(input_folder, 'done.txt'), 'w') as fp:
    	fp.close()


def print_help():
    help_message = """
    Usage: python run.py <input_folder>

    Arguments:
    input_folder   Path to the folder containing PNG images to be processed.

    Example:
    python run.py /path/to/input/folder
    """
    print(help_message)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Error: Invalid number of arguments.")
        print_help()
        sys.exit(1)

    input_folder = sys.argv[1]
    model_path = "/home/widu/models/unet_4x_20210402.hdf5" 

    main(input_folder, model_path)
