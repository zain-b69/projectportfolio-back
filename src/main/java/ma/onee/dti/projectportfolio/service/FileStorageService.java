package ma.onee.dti.projectportfolio.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir);
    }

    public String save(Long projetId, MultipartFile file) throws IOException {

        Path projectFolder = root.resolve("projets")
                                 .resolve(projetId.toString());

        Files.createDirectories(projectFolder);

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());

        String fileName = UUID.randomUUID() + "." + extension;

        Path destination = projectFolder.resolve(fileName);

        Files.copy(file.getInputStream(), destination);

        return destination.toString();
    }

    public Resource load(String chemin) {

        Path path = Paths.get(chemin);

        try {
            return new UrlResource(path.toUri());
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Chemin de fichier invalide", ex);
        }
    }

    public void delete(String chemin) throws IOException {

        Files.deleteIfExists(Paths.get(chemin));

    }
}
