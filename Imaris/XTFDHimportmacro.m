%  XTension to import processed crops from WIDU into an IMS Video (2d+time)
%  
%  Revision: 20221206
%
%  Description: Imports processed slices from WIDU (i.e. analyzed on Google COLAB or others).
%    <CustomTools>
%      <Menu name="IRB - Widefield">
%        <Item name="WID-U  Import macro" icon="I"
%        tooltip="Import macro">
%          <Command>MatlabXT::XTFDHimportmacro(%i)</Command>
%        </Item>
%      </Menu>
%    </CustomTools>

%todo chiedere dove salvare lo zip
function XTFDHimportmacro(aImarisApplicationID)
%% SETTINGS
PATH_PROCESSED_CROPS = 'L:\Lab208\PaolaWTKO\TEMP_RESULTS\'; %Path where to save temporary files
W_CROP = 56; %Size of the crops in pixel. Must match the one of the U-NET. Default for WID-U is 56.
H_CROP = 56;

%% INITIALIZATION
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
[PATH_IMS, FN_IMS, ~] = fileparts(char(fn_ims));

[FN_ZIP,PATH_ZIP] = uigetfile('*.zip','Select PROCESSED zip file...');

aDataSet = vImarisApplication.GetDataSet.Clone;
dataset_size = [aDataSet.GetSizeX, aDataSet.GetSizeY, aDataSet.GetSizeZ, aDataSet.GetSizeC, aDataSet.GetSizeT];

W = dataset_size(1); %556
H = dataset_size(2); %556
Z = dataset_size(3); %18
C = dataset_size(4); %3
T = dataset_size(5); %60

outputChannelName='Result';

z_stack_fdh = zeros(W,H,T,'uint16');

%% Extract slices
h = waitbar(0, 'Extracting slices... ');
unzip([PATH_ZIP,'\',FN_ZIP], PATH_PROCESSED_CROPS);

W2 = floor(W_CROP/2);
H2 = floor(H_CROP/2);
load([PATH_PROCESSED_CROPS, 'coords.mat']);
count_crops = size(associated_coordinates, 1);
tic;
%% Read results
for cc = 1:count_crops
    waitbar(cc/count_crops, h);
    I = imread([PATH_PROCESSED_CROPS,'crop_', num2str(cc, '%06d'),'.png.png']);
    I = imresize(I,[H_CROP,W_CROP]);
    sx = associated_coordinates(cc, 2);
    ex = associated_coordinates(cc, 3);
    sy = associated_coordinates(cc, 4);
    ey = associated_coordinates(cc, 5);
    tt = associated_coordinates(cc, 6);
    z_stack_fdh(sy:ey, sx:ex, tt) = I;
end


%% Creating a new channel
aDataSet.SetSizeC(C + 1);
aDataSet.SetChannelName(C, outputChannelName);
aDataSet.SetChannelColorRGBA(C, 16711935);
aDataSet.SetChannelRange(C, 0, 255);
%% Saving results to Imaris
h = waitbar(0.5, 'Saving results to Imaris...');
for tt=1:T
    aDataSet.SetDataSliceShorts(uint16(z_stack_fdh(:,:,tt)),  0, C,  tt-1);
    waitbar(tt/T, h);
end
vImarisApplication.SetDataSet(aDataSet);
close(h);
close all;
clear all;
return;
end

