package cn.itweet.modules.admin.document.service;

import cn.itweet.common.exception.SystemException;
import cn.itweet.common.utils.TimeMillisUtils;
import cn.itweet.modules.admin.document.entiry.Document;
import cn.itweet.modules.admin.document.repository.DocumentRepository;
import cn.itweet.modules.admin.document.utils.StorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.stream.Stream;

/**
 * Created by whoami on 22/04/2017.
 */
@Service
public class StorageServiceImpl implements StorageService {

    @Autowired
    private DocumentRepository documentRepository;

    private final Path rootLocation;

    @Autowired
    public StorageServiceImpl(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocation());
    }

    @Override
    public void store(MultipartFile file,String path) throws SystemException {

        File f = new File(path);

        if (!f.exists()) {
            f.mkdirs();
        }

        String filename = file.getOriginalFilename();

        String suffix = filename.substring(filename.lastIndexOf(".")+1,filename.length());
        String ruleFilename = TimeMillisUtils.getTimeMillis()+"."+suffix;

        Path rootLocation = Paths.get(path);

        try {
            if (file.isEmpty()) {
                throw  new SystemException("Failed to store empty file " + filename);
            }
            Files.copy(file.getInputStream(), rootLocation.resolve(ruleFilename));
        } catch (IOException e) {
            throw new SystemException("Failed to store file " + filename, e);
        }

        Document document = new Document();
        document.setDate(new Date());
        document.setFilename(filename);
        document.setRuleFilename(ruleFilename);
        document.setType(suffix);

        documentRepository.save(document);
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(path -> this.rootLocation.relativize(path));
        } catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }

    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if(resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new StorageFileNotFoundException("Could not read file: " + filename);

            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }

    @Override
    public void init() {
        try {
            Files.createDirectory(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }

}
