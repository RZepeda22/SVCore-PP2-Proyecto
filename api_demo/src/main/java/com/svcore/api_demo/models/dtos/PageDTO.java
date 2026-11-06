package com.svcore.api_demo.models.dtos;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageDTO<T> {

	private List<T> content;
	private int page;
	private int size;
	private long totalElements;
	private int totalPages;

	public static <T> PageDTO<T> from(Page<T> page) {
		return new PageDTO<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
				page.getTotalPages());
	}
}
