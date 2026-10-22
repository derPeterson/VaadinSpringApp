package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.RoleRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

/** Delegation contract, without application startup or persistence. */
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {
  @Mock private RoleRepository repository;
  private RoleService service;

  @BeforeEach
  void setUp() {
    service = new RoleService(repository);
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void foundRoleReturnsTheSameOptionalAndUnmodifiedEntity(RoleType name) {
    RoleEntity entity = RoleEntity.builder().id(17L).name(name).build();
    Optional<RoleEntity> result = Optional.of(entity);
    when(repository.findByName(name)).thenReturn(result);

    assertSame(result, service.findByName(name));
    assertEquals(17L, entity.getId());
    assertSame(name, entity.getName());
    assertNull(entity.getUserEntities());
    verify(repository).findByName(name);
    verifyNoMoreInteractions(repository);
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void missingRoleRemainsEmptyWithoutCreatingADefault(RoleType name) {
    when(repository.findByName(name)).thenReturn(Optional.empty());

    assertTrue(service.findByName(name).isEmpty());

    verify(repository).findByName(name);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void nullNameIsDelegatedWithoutServiceValidation() {
    when(repository.findByName(null)).thenReturn(Optional.empty());

    assertTrue(service.findByName(null).isEmpty());

    verify(repository).findByName(null);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void repositoryOutageIsPropagatedUnchangedWithoutRetry() {
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("isolated test failure");
    when(repository.findByName(RoleType.ROLE_USER)).thenThrow(failure);

    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> service.findByName(RoleType.ROLE_USER)));

    verify(repository).findByName(RoleType.ROLE_USER);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void nonUniqueResultIsNotDisguisedAsAMissingRole() {
    IncorrectResultSizeDataAccessException failure =
        new IncorrectResultSizeDataAccessException(1, 2);
    when(repository.findByName(RoleType.ROLE_ADMIN)).thenThrow(failure);

    assertSame(
        failure,
        assertThrows(
            IncorrectResultSizeDataAccessException.class,
            () -> service.findByName(RoleType.ROLE_ADMIN)));

    verify(repository).findByName(RoleType.ROLE_ADMIN);
    verifyNoMoreInteractions(repository);
  }
}
