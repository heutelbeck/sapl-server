package io.sapl.server.ce.service.pdpconfiguration;

import io.sapl.interpreter.combinators.PolicyDocumentCombiningAlgorithm;
import io.sapl.server.ce.model.pdpconfiguration.SelectedCombiningAlgorithm;
import io.sapl.server.ce.pdp.PDPConfigurationPublisher;
import io.sapl.server.ce.persistence.SelectedCombiningAlgorithmRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CombiningAlgorithmServiceTests {
    private SelectedCombiningAlgorithmRepository selectedCombiningAlgorithmRepository;
    private PDPConfigurationPublisher pdpConfigurationPublisher;

    @BeforeEach
    void beforeEach() {
        selectedCombiningAlgorithmRepository = mock(SelectedCombiningAlgorithmRepository.class);
        pdpConfigurationPublisher = mock(PDPConfigurationPublisher.class);
    }

    @Test
    void init() {
        CombiningAlgorithmService combiningAlgorithmService = getCombiningAlgorithmService();

        SelectedCombiningAlgorithm entity = new SelectedCombiningAlgorithm();
        entity.setId((long)1);
        entity.setSelection(PolicyDocumentCombiningAlgorithm.ONLY_ONE_APPLICABLE);

        when(selectedCombiningAlgorithmRepository.findAll()).thenReturn(
                Collections.singletonList(entity));
        combiningAlgorithmService.init();
        verify(selectedCombiningAlgorithmRepository, times(1)).findAll();
    }

    @Test
    void getSelected() {
        CombiningAlgorithmService combiningAlgorithmService = getCombiningAlgorithmService();

        SelectedCombiningAlgorithm entity = new SelectedCombiningAlgorithm();
        entity.setId((long)1);
        entity.setSelection(PolicyDocumentCombiningAlgorithm.ONLY_ONE_APPLICABLE);

        when(selectedCombiningAlgorithmRepository.findAll()).thenReturn(
                Collections.singletonList(entity));
        PolicyDocumentCombiningAlgorithm selectedCombiningAlgorithm = combiningAlgorithmService.getSelected();
        assertEquals(entity.getSelection(), selectedCombiningAlgorithm);
    }

    @Test
    void getSelected_noExistingEntity() {
        CombiningAlgorithmService combiningAlgorithmService = getCombiningAlgorithmService();

        when(selectedCombiningAlgorithmRepository.findAll()).thenReturn(Collections.emptyList());
        PolicyDocumentCombiningAlgorithm selectedCombiningAlgorithm = combiningAlgorithmService.getSelected();
        assertEquals(CombiningAlgorithmService.DEFAULT, selectedCombiningAlgorithm);
    }

    @Test
    void getSelected_moreThanOneEntity() {
        CombiningAlgorithmService combiningAlgorithmService = getCombiningAlgorithmService();

        SelectedCombiningAlgorithm entity = new SelectedCombiningAlgorithm();
        entity.setId((long)1);
        entity.setSelection(PolicyDocumentCombiningAlgorithm.ONLY_ONE_APPLICABLE);
        SelectedCombiningAlgorithm otherEntity = new SelectedCombiningAlgorithm();
        entity.setId((long)2);
        entity.setSelection(PolicyDocumentCombiningAlgorithm.DENY_OVERRIDES);

        when(selectedCombiningAlgorithmRepository.findAll()).thenReturn(
                Arrays.asList(entity, otherEntity));
        PolicyDocumentCombiningAlgorithm selectedCombiningAlgorithm = combiningAlgorithmService.getSelected();
        assertEquals(entity.getSelection(), selectedCombiningAlgorithm);
    }

    @Test
    void getAvailable() {
        CombiningAlgorithmService combiningAlgorithmService = getCombiningAlgorithmService();

        assertArrayEquals(PolicyDocumentCombiningAlgorithm.values(), combiningAlgorithmService.getAvailable());
    }

    @Test
    void setSelected() {
        CombiningAlgorithmService combiningAlgorithmService = getCombiningAlgorithmService();

        int invocationCounter = 0;
        for (PolicyDocumentCombiningAlgorithm algorithm : PolicyDocumentCombiningAlgorithm.values()) {
            invocationCounter++;

            combiningAlgorithmService.setSelected(algorithm);

            verify(selectedCombiningAlgorithmRepository, times(invocationCounter)).deleteAll();
            verify(selectedCombiningAlgorithmRepository, times(1)).save(new SelectedCombiningAlgorithm(algorithm));
            verify(pdpConfigurationPublisher, times(1)).publishCombiningAlgorithm(algorithm);
        }
    }

    private CombiningAlgorithmService getCombiningAlgorithmService() {
        return new CombiningAlgorithmService(selectedCombiningAlgorithmRepository, pdpConfigurationPublisher);
    }
}
