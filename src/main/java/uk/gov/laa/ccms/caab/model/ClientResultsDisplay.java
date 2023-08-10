package uk.gov.laa.ccms.caab.model;

import lombok.Data;

import java.util.List;


@Data
public class ClientResultsDisplay {

    private List<ClientResultRowDisplay> content;
    private Integer totalPages;
    private Integer totalElements;
    private Integer number;
    private Integer size;

}
