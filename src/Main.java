import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;


public class Main {

    public static void main(String[] args) {

        Path sourceFolder = Paths.get("source");
        Path destinationFolder = Paths.get("destination");


        try {

            WatchService watchService = FileSystems.getDefault().newWatchService();


            sourceFolder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

            System.out.println("File watcher started. Watching " + sourceFolder.toString());

            // Map to keep track of files in the destination folder
            HashMap<Path, Long> destinationFiles = new HashMap<>();

            while (true) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();


                    if (kind == StandardWatchEventKinds.ENTRY_CREATE ||
                            kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        Path changedFilePath = (Path) event.context();
                        Path sourceFilePath = sourceFolder.resolve(changedFilePath);
                        Path destinationFilePath = destinationFolder.resolve(changedFilePath);

                        try {
                            // Copy the changed or new file to the destination folder
                            Files.copy(sourceFilePath, destinationFilePath, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("File copied: " + sourceFilePath + " -> " + destinationFilePath);

                            // Update the destinationFiles map
                            destinationFiles.put(destinationFilePath, sourceFilePath.toFile().lastModified());
                        } catch (IOException e) {

                        }
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        Path deletedFilePath = destinationFolder.resolve((Path) event.context());

                        // Check if the file still exists in the source folder
                        Path correspondingSourceFile = findCorrespondingSourceFile(deletedFilePath, destinationFiles);

                        if (correspondingSourceFile != null) {
                            try {
                                // Delete the file from the destination folder
                                Files.delete(deletedFilePath);
                                System.out.println("File deleted: " + deletedFilePath);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                key.reset();
            }
        } catch (IOException e) {

        }
    }

    private static Path findCorrespondingSourceFile(Path deletedFilePath, Map<Path, Long> destinationFiles) {
        for (Path sourcePath : destinationFiles.keySet()) {
            if (sourcePath.getFileName().equals(deletedFilePath.getFileName())) {
                return sourcePath;
            }
        }
        return null;
    }
}
