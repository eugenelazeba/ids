package com.thomascook.ids.bdd.steps;

import com.jcraft.jsch.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import cucumber.api.java.Before;
import cucumber.api.java.en.When;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public class OnTourSteps {

    private final static Config ftpConfig = ConfigFactory.load().getConfig("ftp");

    private JSch sftpClient;

    @Before
    public void setUp() throws Exception {
        this.sftpClient = new JSch();

        if (StringUtils.isNotBlank(ftpConfig.getString("privateKeyFile"))) {
            IdentityResource identityResource = new IdentityResource(
                    "sftp-int",
                    ftpConfig.getString("privateKeyFile"),
                    ftpConfig.getString("publicKeyFile"),
                    sftpClient);
            this.sftpClient.addIdentity(identityResource, null);
        }
    }

    @When("^booking (\\S+) was uploaded to SFTP")
    public void uploadFileOnSftp(List<String> files) throws Exception {
        Session session = connect(ftpConfig);
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");

        try {
            channel.connect();
            String remotePath = channel.realpath(normalizePath(ftpConfig.getString("path")));
            putBookingFiles(files, channel, remotePath);
        } finally {
            channel.disconnect();
        }
    }

    private void putBookingFiles(List<String> files, ChannelSftp channel, String remotePath) throws SftpException {
        for (String file : files) {
            InputStream bookingFiles = getClass().getResourceAsStream(file);
            channel.put(bookingFiles, remotePath + file);
        }
    }

    private Session connect(Config ftpConfig) throws IOException {
        try {
            Session session = sftpClient.getSession(ftpConfig.getString("user"), ftpConfig.getString("host"),
                    ftpConfig.getInt("port"));
            session.setPassword(ftpConfig.getString("password"));
            if (StringUtils.isBlank(ftpConfig.getString("knownHostsFile"))) {
                session.setConfig("StrictHostKeyChecking", "no");
            }
            session.connect();
            return session;
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    private String normalizePath(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }
}




