package com.sshtools.j2ssh.sftp;

import com.sshtools.j2ssh.io.UnsignedInteger64;

import java.io.IOException;
import java.io.OutputStream;

public class SftpFileOutputStream extends OutputStream {
    SftpFile file;
    UnsignedInteger64 position;

    public SftpFileOutputStream(SftpFile paramSftpFile)
            throws IOException {
        this(paramSftpFile, 0L);
    }

    public SftpFileOutputStream(SftpFile paramSftpFile, long paramLong)
            throws IOException {
        if (paramSftpFile.getHandle() == null)
            throw new IOException("The file does not have a valid handle!");
        if (paramSftpFile.getSFTPSubsystem() == null)
            throw new IOException("The file is not attached to an SFTP subsystem!");
        this.file = paramSftpFile;
        this.position = new UnsignedInteger64("" + paramLong);
    }

    public void write(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
            throws IOException {
        int i = 0;
        int m = (int) this.file.getSFTPSubsystem().maximumPacketSize();
        while (i < paramInt2) {
            int k = (int) this.file.getSFTPSubsystem().availableWindowSpace() < m ? (int) this.file.getSFTPSubsystem().availableWindowSpace() : m;
            int j = k < paramInt2 - i ? k : paramInt2 - i;
            this.file.getSFTPSubsystem().writeFile(this.file.getHandle(), this.position, paramArrayOfByte, paramInt1 + i, j);
            this.position = UnsignedInteger64.add(this.position, j);
            i += j;
        }
    }

    public void write(int paramInt)
            throws IOException {
        byte[] arrayOfByte = new byte[1];
        arrayOfByte[0] = (byte) paramInt;
        this.file.getSFTPSubsystem().writeFile(this.file.getHandle(), this.position, arrayOfByte, 0, 1);
        this.position = UnsignedInteger64.add(this.position, 1);
    }

    public void close()
            throws IOException {
        this.file.close();
    }

    protected void finalize()
            throws IOException {
        if (this.file.getHandle() != null)
            close();
    }
}