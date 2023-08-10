package uk.gov.laa.ccms.caab.model;

import java.util.List;



public class ClientResultsDisplay {

    private List<ClientResultRowDisplay> content;
    private Integer totalPages;
    private Integer totalElements;
    private Integer number;
    private Integer size;

    public List<ClientResultRowDisplay> getContent() {
        return content;
    }

    public void setContent(List<ClientResultRowDisplay> content) {
        this.content = content;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Integer totalElements) {
        this.totalElements = totalElements;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }


}
