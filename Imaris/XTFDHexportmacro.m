%
%  XTension to export an IMS Video (2d+time) as crops for WIDU
%  
%  Revision: 20221206
%
%  Description: Exports Slices for processing with WIDU on Google COLAB or others.
%
%    <CustomTools>
%      <Menu name="IRB - Widefield">
%        <Item name="WID-U Export macro" icon="I"
%        tooltip="Export macro">
%          <Command>MatlabXT::XTFDHexportmacro(%i)</Command>
%        </Item>
%      </Menu>
%    </CustomTools>

function XTFDHexportmacro(aImarisApplicationID)
%% SETTINGS
PATH_SAVE_CROPS = 'L:\Lab208\PaolaWTKO\TEMP\'; %Path where to save temporary files
W_CROP = 56; %Size of the crops in pixel. Must match the one of the U-NET. Default for WID-U is 56.
H_CROP = 56;

%% INITIALIZATION
system(['del ',PATH_SAVE_CROPS,'*.* /s /q'])
if isa(aImarisApplicationID, 'Imaris.IApplicationPrxHelper')
    vImarisApplication = aImarisApplicationID;
else
    javaaddpath ImarisLib.jar;
    vImarisLib = ImarisLib;
    if ischar(aImarisApplicationID)
        aImarisApplicationID = round(str2double(aImarisApplicationID));
    end
    vImarisApplication = vImarisLib.GetApplication(aImarisApplicationID);
end
[dir, fn, ext] = fileparts(mfilename('fullpath'));

fn_ims = vImarisApplication.GetCurrentFileName;
[PATH_IMS, FN_ZIP, ~] = fileparts(char(fn_ims));
FN_ZIP = [FN_ZIP,'-widu-exported.zip'];
[FN_ZIP,PATH_ZIP] = uiputfile('*.zip','Save exported images as...',FN_ZIP);


aDataSet = vImarisApplication.GetDataSet.Clone;
dataset_size = [aDataSet.GetSizeX, aDataSet.GetSizeY, aDataSet.GetSizeZ, aDataSet.GetSizeC, aDataSet.GetSizeT];

W = dataset_size(1); %556
H = dataset_size(2); %556
Z = dataset_size(3); %18
C = dataset_size(4); %3
T = dataset_size(5); %60

outputChannelName='Result';
channel_to_use = inputdlg(['Channel with transmitted light? [1-',num2str(C),']']);
channel_to_use = str2num(channel_to_use{1});

z_stack = zeros(W,H,T);
z_stack_fdh = zeros(W,H,T,'uint16');

h = waitbar(0, 'Getting data form Imaris... ');
tic;
for tt = 1:T
    I = aDataSet.GetDataSliceFloats(0, channel_to_use-1, tt-1);
    z_stack(:,:,tt) = uint16(I);
    waitbar(tt/T, h);
end
toc;
close(h);


%% Extract slices
z_stack = uint16(z_stack);
W2 = floor(W_CROP/2);
H2 = floor(H_CROP/2);

h = waitbar(0, 'Extracting slices... ');

associated_coordinates = zeros(9999,6); %id,x1,x2,y1,y2,time
count_crops = 0;
tic;
for tt = 1:T
    waitbar(tt/T, h);
    I = imadjust(z_stack(:,:,tt));
    for curr_x = H2+1:H_CROP:H-H2
        for curr_y = W2+1:W_CROP:W-W2
            count_crops = count_crops + 1;
            associated_coordinates(count_crops, 1) = count_crops;
            associated_coordinates(count_crops, 2) = curr_x-W2+1;
            associated_coordinates(count_crops, 3) = curr_x+W2;
            associated_coordinates(count_crops, 4) = curr_y-H2+1;
            associated_coordinates(count_crops, 5) = curr_y+H2;
            associated_coordinates(count_crops, 6) = tt;
            
            P = I(curr_y-H2+1:curr_y+H2, curr_x-W2+1:curr_x+W2);
            P = imresize(P, [H_CROP*4, W_CROP*4]);
            Q = im2double(P);
            imwrite(Q, [PATH_SAVE_CROPS, 'crop_', num2str(count_crops, '%06d'), '.png']);
        end
    end
    
end
save([PATH_SAVE_CROPS, 'coords.mat'], 'associated_coordinates');
toc;
close(h);
h = waitbar(0.8, 'Zipping slices... ');
delete([PATH_ZIP,'\',FN_ZIP]);
zip([PATH_ZIP,'\',FN_ZIP], PATH_SAVE_CROPS);
close(h);
close all;
clear all;
return;
end