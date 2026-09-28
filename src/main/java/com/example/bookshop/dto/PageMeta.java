/*
 ^ TUTORIAL 10 — the "meta" block for paginated lists

 ? Fills the envelope's meta slot (empty since tutorial 07) with the
 ? same fields as the node-mongo-helper reference, so clients can
 ? build pagers without counting anything themselves.
 ? page is 1-BASED here (client-friendly); Spring's Page is 0-based
 ? internally - the service converts.
*/
package com.example.bookshop.dto;

import org.springframework.data.domain.Page;

/**
 * Pagination details returned in the API response's meta field.
 *
 * | Key              | Why we use it                                      |
 * |------------------|----------------------------------------------------|
 * | total            | Reports the total number of matching rows          |
 * | page             | Reports a client-friendly, one-based page number   |
 * | limit            | Reports the number of rows returned per page       |
 * | totalPages       | Reports the number of available pages              |
 * | hasNext/hasPrev  | Lets clients enable or disable paging controls     |
 * | from(Page)       | Converts Spring's zero-based Page metadata          |
 */
public record PageMeta(long total, int page, int limit, int totalPages,
		boolean hasNextPage, boolean hasPrevPage) {

	public static PageMeta from(Page<?> result) {
		int page = result.getNumber() + 1;
		return new PageMeta(
				result.getTotalElements(),
				page,
				result.getSize(),
				Math.max(result.getTotalPages(), 1),
				result.hasNext(),
				result.hasPrevious());
	}
}
