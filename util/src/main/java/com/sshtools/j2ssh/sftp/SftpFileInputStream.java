package com.sshtools.j2ssh.sftp;

import com.sshtools.j2ssh.io.UnsignedInteger64;

import java.io.IOException;
import java.io.InputStream;

public class SftpFileInputStream extends InputStream {
    SftpFile file;
    UnsignedInteger64 position;

    public SftpFileInputStream(SftpFile paramSftpFile) throws IOException {
        this(paramSftpFile, 0L);
    }

    public SftpFileInputStream(SftpFile paramSftpFile, long paramLong)
            throws IOException {
        if (paramSftpFile.getHandle() == null)
            throw new IOException("The file does not have a valid handle!");
        if (paramSftpFile.getSFTPSubsystem() == null)
            throw new IOException("The file is not attached to an SFTP subsystem!");
        this.file = paramSftpFile;
        this.position = new UnsignedInteger64("" + paramLong);
    }

    public long getPosition() {
        return this.position.longValue();
    }

    public void setPosition(long paramLong) {
        this.position = new UnsignedInteger64("" + paramLong);
    }

    public int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2) throws IOException {
        int i = this.file.getSFTPSubsystem().readFile(this.file.getHandle(), this.position, paramArrayOfByte, paramInt1, paramInt2);
        if (i > 0)
            this.position = UnsignedInteger64.add(this.position, i);
        return i;
    }

    public int read() throws IOException {
        byte[] arrayOfByte = new byte[1];
        int i = this.file.getSFTPSubsystem().readFile(this.file.getHandle(), this.position, arrayOfByte, 0, 1);
        this.position = UnsignedInteger64.add(this.position, i);
        return arrayOfByte[0] & 0xFF;
    }

    public void close() throws IOException {
        this.file.close();
    }

    protected void finalize() throws IOException {
        if (this.file.getHandle() != null)
            close();
    }
}