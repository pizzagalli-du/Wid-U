/**************************************************************************
 *
 * Copyright (C) 2022   Paola Antonello, 
 *                      Diego Morone, 
 *                      Marcus Thelen,
 *                      Rolf Krause,
 *                      Diego Ulisse Pizzagalli  
 * 
 *	  Institute for Research in Biomedicine
*	  Switzerland
*
*    Graduate School for Cellular and Molecular Sciences,
*    University of Bern, Switzerland
*
*    Euler Institute, Università della Svizzera Italiana,
*    Switzerland
* 	
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in the
*    documentation and/or other materials provided with the distribution.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*
**************************************************************************/

package ch.irb.WidU;

import ij.IJ;
import ij.Prefs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.jcraft.jsch.*;

/**
 * Utilities for SSH connection, sending images, running commands and testing connection
 * 
 * @author Diego Morone
 */
public class SSHConnection {

    private Session session;
    private String folder;

    /**
     * 
     * @param hostname
     * @param port
     * @param username
     * @param folder
     */
    public SSHConnection(String hostname, Integer port, String username, String folder) {
        try{ 
            this.session = newsession(hostname, port, username);
        } catch (Exception e) {
            IJ.error("Wid-U","Error during connection: "+ e.getMessage());
        }
        this.folder = folder;
    }

    /**
     * 
     * @param blob
     */
    public void sendBlob(Blob blob) {

        Channel channel = null;
        ChannelSftp channelSftp = null;

        String rawfolder = Paths.get(this.folder, blob.getID()).toString().replace(System.getProperty("file.separator"), "/"); 

        try {
            
            channel = session.openChannel("sftp");
            channel.connect();
    
            channelSftp = (ChannelSftp) channel;
            
            channelSftp.mkdir(rawfolder);
            channelSftp.cd(rawfolder); 

            HashMap<String, byte[]> blobtiles = blob.getRawTiles();
            for (Map.Entry<String, byte[]> entry : blobtiles.entrySet()) {
                String remotepath =  Paths.get(rawfolder, entry.getKey()).toString().replace(System.getProperty("file.separator"), "/");
                IJ.log("Sending file: " + entry.getKey());
                ByteArrayInputStream f = new ByteArrayInputStream(entry.getValue());
                channelSftp.put(f, remotepath);
            }

        } catch (Exception e) {
            IJ.error("Wid-U", "File transfer test failed. Error: "+ e.getMessage());
        } finally {
            channelSftp.exit();
            IJ.log("SFTP Channel exited.");
            channel.disconnect();
            IJ.log("Channel disconnected.");
        }
        IJ.log("Files sent OK");


    }

    /**
     * 
     * @param blob
     * @param command
     */
    public boolean exec(Blob blob, String command) {

        StringBuilder outputBuffer = new StringBuilder();

        Channel channel = null;
        ChannelSftp channelSftp = null;
        ChannelExec channelExec = null;

        String blobfolder = Paths.get(this.folder, blob.getID()).toString().replace(System.getProperty("file.separator"), "/"); 
        command = command + " " + blobfolder + "/";
        IJ.log(String.format("Running %s", command));

        Boolean waitmore = true;

        try {
            
            channel = session.openChannel("exec");
            channelExec = ((ChannelExec) channel);
            IJ.log("Add process to job queue");

            channelExec.setCommand(command);
            InputStream commandOutput = channel.getInputStream();

            channel.connect();
            int readByte = commandOutput.read();

            while(readByte != 0xffffffff) {
                outputBuffer.append((char)readByte);
                readByte = commandOutput.read();
            }

            while(true){
                if(channelExec.isClosed()){
                    break;
                }
            }
        } catch (IOException e) {
            IJ.error("Wid-U", e.getMessage());
        } catch (JSchException je) {
            IJ.error("Wid-U", je.getMessage());
        } finally {
            IJ.log(outputBuffer.toString());
            channel.disconnect();
        }

        // Every 5s check for file UUID/done.txt. If there, collect segmented files
        try {
            
            channel = session.openChannel("sftp");
            channel.connect();
    
            channelSftp = (ChannelSftp) channel;

            channelSftp.cd(blobfolder);

            String path = Paths.get(this.folder, blob.getID(), "done.txt").toString().replace(System.getProperty("file.separator"), "/");
            
            while (waitmore) {
                try{Thread.sleep(5000);}catch(Exception ee){} // wait 5s
                IJ.log("Please wait...");

                try {
                    channelSftp.lstat(path);
                    waitmore = false;
                } catch (SftpException e){
                    if(e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE){
                       waitmore = true;
                    } else {
                        // something else went wrong
                        throw e;
                    }
                }
            }

        } catch (Exception e) {
            IJ.error("Wid-U", "Command run failed. Error: "+ e.getMessage());
        } finally {
            channelSftp.exit();
            channel.disconnect();
            IJ.log("Segmentation OK");
        }

        return true;
        
    }

    /**
     * 
     * @param blob
     */
    public void getremoteBlob(Blob blob) {
        Channel channel = null;
        ChannelSftp channelSftp = null;

        String segmentedfolder = Paths.get(this.folder, blob.getID(), "results").toString().replace(System.getProperty("file.separator"), "/");

        try {
            
            channel = session.openChannel("sftp");
            channel.connect();
    
            channelSftp = (ChannelSftp) channel;
              
            channelSftp.cd(segmentedfolder); 

            HashMap<String, byte[]> blobtiles = blob.getRawTiles();
            for (Map.Entry<String, byte[]> entry : blobtiles.entrySet()) {
                String key = entry.getKey();
                String remotepath =  Paths.get(segmentedfolder, key).toString().replace(System.getProperty("file.separator"), "/");
                IJ.log("Receiving file: " + key);
                InputStream f  = channelSftp.get(remotepath);
                byte[] tmp = readAllBytes(f);
                blob.addSegmentedTile(tmp, key);
            }

        } catch (Exception e) {
            IJ.error("Wid-U", "File transfer test failed. Error: "+ e.getMessage());
        } finally {
            channelSftp.exit();
            IJ.log("SFTP Channel exited.");
            channel.disconnect();
            IJ.log("Channel disconnected.");
        }
        IJ.log("Files received OK");
    }

    /**
     * 
     * @param inblob
     */
    public void deleteremoteBlob(Blob inblob) {
        Channel channel = null;
        ChannelSftp channelSftp = null;

        String path = Paths.get(this.folder, inblob.getID()).toString().replace(System.getProperty("file.separator"), "/");

        try {
            
            channel = session.openChannel("sftp");
            channel.connect();
    
            channelSftp = (ChannelSftp) channel;
              
            recursiveFolderDelete(channelSftp, path);

        } catch (Exception e) {
            IJ.error("Wid-U", "File transfer test failed. Error: "+ e.getMessage());
        } finally {
            channelSftp.exit();
            IJ.log("SFTP Channel exited.");
            channel.disconnect();
            IJ.log("Channel disconnected.");
        }
        IJ.log("Files deleted OK");
    }

    /*
     * 
     */
    @SuppressWarnings("unchecked")
    private static void recursiveFolderDelete(ChannelSftp channelSftp, String path) throws SftpException {
    
        // List source directory structure.
        Collection<ChannelSftp.LsEntry> fileAndFolderList = channelSftp.ls(path);
    
        // Iterate objects in the list to get file/folder names.
        for (ChannelSftp.LsEntry item : fileAndFolderList) {
            if (!item.getAttrs().isDir()) {
                channelSftp.rm(path + "/" + item.getFilename()); // Remove file.
            } else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) { // If it is a subdir.
                try {
                    // removing sub directory.
                    channelSftp.rmdir(path + "/" + item.getFilename());
                } catch (Exception e) { // If subdir is not empty and error occurs.
                    // Do lsFolderRemove on this subdir to enter it and clear its contents.
                    recursiveFolderDelete(channelSftp, path + "/" + item.getFilename());
                }
            }
        }
        channelSftp.rmdir(path); // delete the parent directory after empty
    }

    /**
     * Disconnect open session
     */
    public void disconnect() {
        session.disconnect();
        IJ.log("SSH connection terminated");
    }
  
    /**
     * 
     * @param hostname
     * @param port
     * @param username
     * @return
     * @throws JSchException
     */
    private Session newsession(String hostname, Integer port, String username) throws JSchException {
        IJ.log("SSH connection to "+hostname+", port: "+port+" with username "+username);

        JSch jsch=new JSch();
        
        JSch.setConfig("StrictHostKeyChecking", "no");

        Session session = null;
        
        session=jsch.getSession(username, hostname, port);

        // Establish file transfer with compression if possible
        session.setConfig("compression.s2c", "zlib@openssh.com,zlib,none");
        session.setConfig("compression.c2s", "zlib@openssh.com,zlib,none");
        session.setConfig("compression_level", "9");

        // Check for auth method. Fallback is password
        Boolean needpassword = (Prefs.get("ch.irb.widu.auth", "Password") == "Password" )? true : false;

        if (needpassword) {
            String password = promptPassphrase("Password for "+hostname);
            session.setPassword(password); 
        } else {
            String rsakey = Prefs.get("ch.irb.widu.rsapath", "");
            if (rsakey != "") {
                try {
                    jsch.addIdentity(rsakey);
                } catch (JSchException e) {
                    try {
                        String rsapass = promptPassphrase("Password for RSA key");
                        jsch.addIdentity(rsakey, rsapass);
                    } catch (JSchException f) {

                    }
                }
            } else {
                String password = promptPassphrase("Password for "+hostname);
                session.setPassword(password); 
            } 
        }

        session.connect();
        IJ.log("SSH connection established");

        return session;

    }

    /*
     * 
     */
    private String promptPassphrase(String message){
        JTextField passphraseField=(JTextField)new JPasswordField(20);
        String passphrase = null;

        Object[] ob={passphraseField};
        int result= JOptionPane.showConfirmDialog(null, ob, message,JOptionPane.OK_CANCEL_OPTION);
        if(result==JOptionPane.OK_OPTION){
        passphrase=passphraseField.getText();

        }
        return passphrase;
    }

    /**
     * 
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        final int bufLen = 4 * 0x400; // 4KB
        byte[] buf = new byte[bufLen];
        int readLen;
        IOException exception = null;
   
        try {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
                    outputStream.write(buf, 0, readLen);
   
                return outputStream.toByteArray();
            }
        } catch (IOException e) {
            exception = e;
            throw e;
        } finally {
            if (exception == null) inputStream.close();
            else try {
                inputStream.close();
            } catch (IOException e) {
                exception.addSuppressed(e);
            }
        }
    }
}
