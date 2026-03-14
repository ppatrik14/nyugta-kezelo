package hu.szamlazz.receipt.mapper;

import hu.szamlazz.receipt.dto.ReceiptItemDto;
import hu.szamlazz.receipt.dto.ReceiptResponse;
import hu.szamlazz.receipt.entity.Receipt;
import hu.szamlazz.receipt.entity.ReceiptItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper a Receipt entity és a DTO-k között.
 */
@Component
public class ReceiptMapper {

    /**
     * Receipt entitás → ReceiptResponse DTO.
     */
    public ReceiptResponse toResponse(Receipt entity) {
        return ReceiptResponse.builder()
                .id(entity.getId())
                .szamlazzId(entity.getSzamlazzId())
                .nyugtaszam(entity.getNyugtaszam())
                .hivasAzonosito(entity.getHivasAzonosito())
                .elotag(entity.getElotag())
                .fizmod(entity.getFizmod())
                .penznem(entity.getPenznem())
                .kelt(entity.getKelt())
                .tipus(entity.getTipus())
                .stornozott(entity.isStornozott())
                .megjegyzes(entity.getMegjegyzes())
                .teszt(entity.isTeszt())
                .totalNetto(entity.getTotalNetto())
                .totalAfa(entity.getTotalAfa())
                .totalBrutto(entity.getTotalBrutto())
                .createdAt(entity.getCreatedAt())
                .tetelek(toItemDtoList(entity.getTetelek()))
                .build();
    }

    /**
     * Receipt entitások listája → ReceiptResponse DTO-k listája.
     */
    public List<ReceiptResponse> toResponseList(List<Receipt> entities) {
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * ReceiptItem entitás → ReceiptItemDto.
     */
    public ReceiptItemDto toItemDto(ReceiptItem entity) {
        return ReceiptItemDto.builder()
                .megnevezes(entity.getMegnevezes())
                .mennyiseg(entity.getMennyiseg())
                .mennyisegiEgyseg(entity.getMennyisegiEgyseg())
                .nettoEgysegar(entity.getNettoEgysegar())
                .afakulcs(entity.getAfakulcs())
                .netto(entity.getNetto())
                .afa(entity.getAfa())
                .brutto(entity.getBrutto())
                .build();
    }

    /**
     * ReceiptItemDto → ReceiptItem entitás (nyugta nélkül – azt a service állítja be).
     */
    public ReceiptItem toItemEntity(ReceiptItemDto dto) {
        return ReceiptItem.builder()
                .megnevezes(dto.getMegnevezes())
                .mennyiseg(dto.getMennyiseg())
                .mennyisegiEgyseg(dto.getMennyisegiEgyseg())
                .nettoEgysegar(dto.getNettoEgysegar())
                .afakulcs(dto.getAfakulcs())
                .netto(dto.getNetto())
                .afa(dto.getAfa())
                .brutto(dto.getBrutto())
                .build();
    }

    private List<ReceiptItemDto> toItemDtoList(List<ReceiptItem> items) {
        if (items == null) return List.of();
        return items.stream()
                .map(this::toItemDto)
                .collect(Collectors.toList());
    }
}
