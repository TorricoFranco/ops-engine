package com.franco.opsengine.evaluation;

import com.franco.opsengine.evaluation.adapter.cli.DatasetReader;
import com.franco.opsengine.evaluation.domain.LabelledTicket;
import com.franco.opsengine.shared.domain.Skill;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatasetReaderTest {

    private static final String HEADER = "id,title,description,expected_skill\n";

    @Test
    void readsALabelledRow() {
        List<LabelledTicket> dataset = DatasetReader.read(
                HEADER + "EV-01,Cobros,Dos facturas el mismo mes,BILLING\n");

        assertThat(dataset).hasSize(1);
        assertThat(dataset.getFirst().id()).isEqualTo("EV-01");
        assertThat(dataset.getFirst().expectedSkill()).isEqualTo(new Skill("BILLING"));
    }

    @Test
    void keepsCommasInsideQuotedDescriptions() {
        List<LabelledTicket> dataset = DatasetReader.read(
                HEADER + "EV-01,Cobros,\"Dos facturas, un solo mes, mismo importe\",BILLING\n");

        assertThat(dataset.getFirst().description())
                .isEqualTo("Dos facturas, un solo mes, mismo importe");
    }

    @Test
    void readsALiteralQuoteWrittenAsTwo() {
        List<LabelledTicket> dataset = DatasetReader.read(
                HEADER + "EV-01,Error,\"Dice \"\"acceso denegado\"\" al entrar\",ACCESS_IDENTITY\n");

        assertThat(dataset.getFirst().description()).isEqualTo("Dice \"acceso denegado\" al entrar");
    }

    @Test
    void ignoresCommentsAndBlankLines() {
        List<LabelledTicket> dataset = DatasetReader.read(
                "# un comentario\n\n" + HEADER + "\nEV-01,T,D,BILLING\n# otro\n");

        assertThat(dataset).hasSize(1);
    }

    @Test
    void acceptsAHeaderWithSpacesOrUpperCase() {
        List<LabelledTicket> dataset = DatasetReader.read(
                "ID, Title, Description, Expected_Skill\nEV-01,T,D,BILLING\n");

        assertThat(dataset).hasSize(1);
    }

    @Test
    void normalizesTheSkillCode() {
        List<LabelledTicket> dataset = DatasetReader.read(HEADER + "EV-01,T,D,billing\n");

        assertThat(dataset.getFirst().expectedSkill()).isEqualTo(new Skill("BILLING"));
    }

    @Test
    void reportsUnlabelledRowsWithTheirLineNumberInsteadOfSkippingThem() {
        // Saltearlas daría un resultado que parece válido sobre un dataset a medias.
        assertThatThrownBy(() -> DatasetReader.read(
                HEADER + "EV-01,T,D,BILLING\nEV-02,T,D,\nEV-03,T,D,\n"))
                .isInstanceOf(DatasetReader.InvalidDatasetException.class)
                .hasMessageContaining("línea 3")
                .hasMessageContaining("línea 4")
                .hasMessageContaining("expected_skill");
    }

    @Test
    void reportsAnInvalidSkillCode() {
        assertThatThrownBy(() -> DatasetReader.read(HEADER + "EV-01,T,D,NO EXISTE\n"))
                .isInstanceOf(DatasetReader.InvalidDatasetException.class)
                .hasMessageContaining("línea 2");
    }

    @Test
    void reportsARowWithTheWrongNumberOfColumns() {
        assertThatThrownBy(() -> DatasetReader.read(HEADER + "EV-01,T,BILLING\n"))
                .isInstanceOf(DatasetReader.InvalidDatasetException.class)
                .hasMessageContaining("4 columnas");
    }

    @Test
    void reportsAnUnclosedQuote() {
        assertThatThrownBy(() -> DatasetReader.read(HEADER + "EV-01,T,\"sin cerrar,BILLING\n"))
                .isInstanceOf(DatasetReader.InvalidDatasetException.class);
    }

    @Test
    void rejectsAMissingHeader() {
        assertThatThrownBy(() -> DatasetReader.read("EV-01,T,D,BILLING\n"))
                .isInstanceOf(DatasetReader.InvalidDatasetException.class)
                .hasMessageContaining("encabezado");
    }

    @Test
    void rejectsAnEmptyDataset() {
        assertThatThrownBy(() -> DatasetReader.read(""))
                .isInstanceOf(DatasetReader.InvalidDatasetException.class);
    }

    @Test
    void rejectsAHeaderWithNoRows() {
        assertThatThrownBy(() -> DatasetReader.read(HEADER))
                .isInstanceOf(DatasetReader.InvalidDatasetException.class)
                .hasMessageContaining("ninguna fila");
    }
}
