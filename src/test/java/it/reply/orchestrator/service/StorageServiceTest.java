package it.reply.orchestrator.service;

import it.reply.orchestrator.dal.entity.StoragePathEntity;
import it.reply.orchestrator.dal.repository.StorageRepository;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

public class StorageServiceTest {

  
  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Mock
  StorageRepository storageRepository;

  @InjectMocks
  StorageService storageService = new StorageServiceImpl(storageRepository);

  @Test
  public void addStoragePathNew() throws Exception {

    Mockito.when(storageRepository.findByStoragePath("stringUrl1"))
    .thenReturn(null);

    Mockito.when(storageRepository.save(any(StoragePathEntity.class)))
    .thenReturn(new StoragePathEntity("stringUrl1"));

    assertThat(storageService.addStoragePath("stringUrl1")!=(null));

  }

  @Test
  public void addStoragePathExisting() throws Exception {

    Mockito.when(storageRepository.findByStoragePath("stringUrl1"))
    .thenReturn("stringUrl1");

    Mockito.when(storageRepository.save(new StoragePathEntity("stringUrl1")))
    .thenReturn(new StoragePathEntity("stringUrl1"));

    assertThat(storageService.addStoragePath("stringUrl1") == null);

  }

}
