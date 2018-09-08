package com.moelholm.tools.mediaorganizer;

import com.moelholm.tools.mediaorganizer.filesystem.DropboxFileSystem;
import com.moelholm.tools.mediaorganizer.filesystem.FileSystem;
import com.moelholm.tools.mediaorganizer.filesystem.FileSystemType;
import com.moelholm.tools.mediaorganizer.filesystem.LocalFileSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class BeanConfig {

  @Autowired private Environment environment;

  @Bean
  public ThreadPoolTaskScheduler taskScheduler() {
    return new ThreadPoolTaskScheduler();
  }

  @Bean
  public FileSystem fileSystem() {
    if (FileSystemType.LOCAL
        == FileSystemType.fromString(
            environment.getProperty(MainArgument.FILESYSTEM_TYPE.getArgumentName()))) {
      return new LocalFileSystem();
    } else {
      return new DropboxFileSystem();
    }
  }
}
