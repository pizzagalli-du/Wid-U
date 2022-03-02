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
import java.io.InputStream;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.jcraft.jsch.*;

/**
 * Utilities for SSH connection, sending images and testing connection
 * 
 * @author Diego Morone
 */
public class SSHConnection {

    private Session session;
    private String folder;

    public SSHConnection(String hostname, Integer port, String username, String folder) {
        try{ 
            this.session = newsession(hostname, port, username);
        } catch (Exception e) {
            IJ.error("Wid-U","Error during connection: "+ e.getMessage());
        }
        this.folder = folder;
    }

    public void sendBlob(Blob blob) {

        Channel channel = null;
        ChannelSftp channelSftp = null;

        String s = "test\n";
        String remotepath = "test.txt";
        InputStream send = new ByteArrayInputStream(s.getBytes());

        try {
            
            channel = session.openChannel("sftp");
            channel.connect();
    
            channelSftp = (ChannelSftp) channel;
              
            channelSftp.cd(folder);

            // TODO: send vector<file> through ssh
            // TODO: no space left on remote: delete all and terminate

            channelSftp.put(send, remotepath);

        } catch (Exception e) {
            IJ.error("Wid-U", "File transfer test failed. Error: "+ e.getMessage());
        } finally {
            channelSftp.exit();
            IJ.log("SFTP Channel exited.");
            channel.disconnect();
            IJ.log("Channel disconnected.");
        }
        IJ.log("File sent OK");


    }

    public void exec(String id, String command) {
        // TODO: execute command to run Tensorflow
        // TODO: segmentation aborts: delete all and terminate
        // TODO: every 5s check for file UUID.done. If there, collect segmented files
        
    }

    public void deleteremoteBlob(String id) {
        Channel channel = null;
        ChannelSftp channelSftp = null;

        String remotepath = "test.txt";

        try {
            
            channel = session.openChannel("sftp");
            channel.connect();
    
            channelSftp = (ChannelSftp) channel;
              
            channelSftp.cd(folder);

            // TODO: rm vector<file> through ssh

            channelSftp.rm(remotepath);

        } catch (Exception e) {
            IJ.error("Wid-U", "File transfer test failed. Error: "+ e.getMessage());
        } finally {
            channelSftp.exit();
            IJ.log("SFTP Channel exited.");
            channel.disconnect();
            IJ.log("Channel disconnected.");
        }
        IJ.log("File sent OK");
    }

    public Blob getremoteBlob(String id) {
        Blob blob = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;

        try {
            
            channel = session.openChannel("sftp");
            channel.connect();
    
            channelSftp = (ChannelSftp) channel;
              
            channelSftp.cd(folder);

            // TODO: send vector<file> through ssh
            //channelSftp.get();

        } catch (Exception e) {
            IJ.error("Wid-U", "File transfer test failed. Error: "+ e.getMessage());
        } finally {
            channelSftp.exit();
            IJ.log("SFTP Channel exited.");
            channel.disconnect();
            IJ.log("Channel disconnected.");
        }
        IJ.log("File sent OK");
        return blob;
    }

    /**
     * 
     * @param remotefolder
     * @return
     */
    public boolean testsend() {

        Channel channel = null;
        ChannelSftp channelSftp = null;

        String s = "test\n";
        String remotepath = "test.txt";
        InputStream send = new ByteArrayInputStream(s.getBytes());

        try {
            
            channel = session.openChannel("sftp");
            channel.connect();
    
            channelSftp = (ChannelSftp) channel;
              
            channelSftp.cd(folder);
            channelSftp.put(send, remotepath);
            channelSftp.rm(remotepath);

        } catch (Exception e) {
            IJ.error("Wid-U", "File transfer test failed. Error: "+ e.getMessage());
        } finally {
            channelSftp.exit();
            IJ.log("SFTP Channel exited.");
            channel.disconnect();
            IJ.log("Channel disconnected.");
        }
        IJ.log("File sent OK");
        return true;
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

}
