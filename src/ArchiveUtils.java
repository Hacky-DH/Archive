package hacky;


import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ArchiveUtils {
    private static final int BUFFER_SIZE = 1024 * 8;

    /*
    * *****************************
    * archive
    * *****************************
     */
    public static void archive(String srcPath, boolean gzip, boolean includeDir) throws Exception {
        archive(new File(srcPath), gzip, includeDir);
    }

    public static void archive(File srcFile, boolean gzip, boolean includeDir) throws Exception {
        String ext = gzip ? ".tar.gz" : ".tar";
        File destPath = FileUtils.getFile(srcFile.getParent(), srcFile.getName() + ext);
        archive(srcFile, destPath, gzip, includeDir);
    }

    public static void archive(String srcPath, String destPath, boolean gzip, boolean includeDir)
            throws Exception {
        archive(new File(srcPath), destPath, gzip, includeDir);
    }

    public static void archive(File srcFile, String destPath, boolean gzip, boolean includeDir) throws Exception {
        archive(srcFile, new File(destPath), gzip, includeDir);
    }

    /**
     * @param srcFile
     * @param destFile
     * @param gzip       true use gzip
     * @param includeDir true use gzip
     * @throws Exception
     */
    public static void archive(File srcFile, File destFile, boolean gzip, boolean includeDir) throws Exception {
        if (destFile.getParentFile().equals(srcFile)) {
            throw new IOException("The path of destFile is not allowed equal to srcPath " + srcFile);
        }
        TarArchiveOutputStream tarOut = null;
        try {
            if (gzip) {
                tarOut = new TarArchiveOutputStream(new GZIPOutputStream(
                        new BufferedOutputStream(new FileOutputStream(destFile))));
            } else {
                tarOut = new TarArchiveOutputStream(new BufferedOutputStream(
                        new FileOutputStream(destFile)));
            }
            if (!srcFile.isDirectory() || includeDir) {
                archive(srcFile, tarOut, "");
            } else {
                for (File file : srcFile.listFiles()) {
                    archive(file, tarOut, "");
                }
            }
        } finally {
            try {
                if (tarOut != null) {
                    tarOut.flush();
                    tarOut.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * archive all files in path recursively or a file
     *
     * @param srcFile  src path
     * @param tarOut   TarArchiveOutputStream
     * @param basePath relative path in tar
     * @throws Exception
     */
    private static void archive(File srcFile, TarArchiveOutputStream tarOut,
                                String basePath) throws Exception {
        if (srcFile.isDirectory()) {
            archiveDir(srcFile, tarOut, basePath);
        } else {
            archiveFile(srcFile, tarOut, basePath);
        }
    }


    /**
     * archive all files in dir recursively
     *
     * @param dir      src dir
     * @param tarOut   TarArchiveOutputStream
     * @param basePath relative path in tar
     * @throws Exception
     */
    private static void archiveDir(File dir, TarArchiveOutputStream tarOut,
                                   String basePath) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) return;
        if (files.length < 1) {
            try {
                TarArchiveEntry entry = new TarArchiveEntry(basePath
                        + dir.getName() + File.separator);
                tarOut.putArchiveEntry(entry);
            } finally {
                try {
                    tarOut.closeArchiveEntry();
                } catch (IOException ignored) {
                }
            }
        }

        for (File file : files) {
            archive(file, tarOut, basePath + dir.getName() + File.separator);
        }
    }

    /**
     * archive file
     *
     * @param file   file
     * @param tarOut TarArchiveOutputStream
     * @param dir    dir of file
     * @throws Exception
     */
    private static void archiveFile(File file, TarArchiveOutputStream tarOut,
                                    String dir) throws Exception {
        BufferedInputStream input = null;
        try {
            TarArchiveEntry entry = new TarArchiveEntry(dir + file.getName());
            entry.setSize(file.length());
            tarOut.putArchiveEntry(entry);
            input = new BufferedInputStream(new FileInputStream(file));
            int count;
            byte data[] = new byte[BUFFER_SIZE];
            while ((count = input.read(data, 0, BUFFER_SIZE)) != -1) {
                tarOut.write(data, 0, count);
            }
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
                tarOut.closeArchiveEntry();
            } catch (IOException ignored) {
            }
        }
    }

    /*
    * *****************************
    * deArchive
    * *****************************
     */
    public static void deArchive(String srcFile, boolean gzip) throws Exception {
        deArchive(new File(srcFile), gzip);
    }

    public static void deArchive(File srcFile, boolean gzip) throws Exception {
        deArchive(srcFile, srcFile.getParent(), gzip);
    }

    public static void deArchive(String srcFile, String destPath, boolean gzip) throws Exception {
        deArchive(new File(srcFile), destPath, gzip);
    }

    public static void deArchive(File srcFile, String destPath, boolean gzip) throws Exception {
        deArchive(srcFile, new File(destPath), gzip);
    }

    /**
     * @param srcFile
     * @param destFile
     * @param gzip     true use gzip
     * @throws Exception
     */
    public static void deArchive(File srcFile, File destFile, boolean gzip) throws Exception {
        TarArchiveInputStream tarInput = null;
        try {
            if (gzip) {
                tarInput = new TarArchiveInputStream(new GZIPInputStream(
                        new BufferedInputStream(new FileInputStream(srcFile))));
            } else {
                tarInput = new TarArchiveInputStream(new BufferedInputStream(
                        new FileInputStream(srcFile)));
            }
            deArchive(destFile, tarInput);
        } finally {
            try {
                if (tarInput != null) {
                    tarInput.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    private static void deArchive(File destFile, TarArchiveInputStream tarInput) throws Exception {
        TarArchiveEntry entry;
        while ((entry = tarInput.getNextTarEntry()) != null) {
            File dirFile = FileUtils.getFile(destFile, entry.getName());
            if (entry.isDirectory()) {
                dirFile.mkdirs();
            } else {
                dirFile.getParentFile().mkdirs();
                deArchiveFile(dirFile, tarInput);
            }
        }
    }

    private static void deArchiveFile(File destFile, TarArchiveInputStream tarInput) throws Exception {
        BufferedOutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream(destFile));
            int count;
            byte data[] = new byte[BUFFER_SIZE];
            while ((count = tarInput.read(data, 0, BUFFER_SIZE)) != -1) {
                output.write(data, 0, count);
            }
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    public static void main(String[] args) {
        PrintStream out = System.out;
        PrintStream err = System.err;
        final String usage = "archive <-c|-x> <archiveFile> <path> [-g] [-i]\narchive <-ca|-xa> <path> [-g] [-i]";
        if (args.length < 3) {
            err.println(usage);
            System.exit(1);
        }
        String archiveFile = "", path = "";
        boolean create = true, gzip = false, includeDir = false, autoCreate = true;
        for (int i = 0; i < args.length; ) {
            if (args[i].trim().equalsIgnoreCase("-ca") || args[i].trim().equalsIgnoreCase("-xa")) {
                autoCreate = args[i].trim().equalsIgnoreCase("-ca");
                if (i + 1 >= args.length) {
                    err.println(usage);
                    System.exit(1);
                }
                path = args[i + 1].trim();
                if (!new File(path).exists()) {
                    err.println("path " + path + " is not exists");
                    System.exit(1);
                }
                i += 2;
            } else if (args[i].trim().equalsIgnoreCase("-c") || args[i].trim().equalsIgnoreCase("-x")) {
                create = args[i].trim().equalsIgnoreCase("-c");
                if (i + 2 >= args.length) {
                    err.println(usage);
                    System.exit(1);
                }
                archiveFile = args[i + 1].trim();
                path = args[i + 2].trim();
                i += 3;
            } else if (args[i].trim().equalsIgnoreCase("-g")) {
                gzip = true;
                ++i;
            } else if (args[i].trim().equalsIgnoreCase("-i")) {
                includeDir = true;
                ++i;
            } else {
                err.println(usage);
                System.exit(1);
            }
        }
        if (autoCreate) {
            try {
                archive(path, gzip, includeDir);
                out.println("archive complete");
                System.exit(0);
            } catch (Exception e) {
                err.println("archive error " + e.getMessage());
                System.exit(2);
            }
        } else {
            try {
                deArchive(path, gzip);
                out.println("deArchive complete");
                System.exit(0);
            } catch (Exception e) {
                err.println("deArchive error " + e.getMessage());
                System.exit(2);
            }
        }
        if (create) {
            try {
                if (!new File(path).exists()) {
                    err.println("path " + path + " is not exists");
                    System.exit(1);
                }
                archive(path, archiveFile, gzip, includeDir);
                out.println("archive complete");
                System.exit(0);
            } catch (Exception e) {
                err.println("archive error " + e.getMessage());
                System.exit(2);
            }
        } else {
            try {
                if (!new File(archiveFile).exists()) {
                    err.println("path " + archiveFile + " is not exists");
                    System.exit(1);
                }
                deArchive(archiveFile, path, gzip);
                out.println("deArchive complete");
                System.exit(0);
            } catch (Exception e) {
                err.println("deArchive error " + e.getMessage());
                System.exit(2);
            }
        }
    }
}
