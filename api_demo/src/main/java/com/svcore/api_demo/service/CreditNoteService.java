package com.svcore.api_demo.service;

import com.svcore.api_demo.models.dtos.CreditNoteDTO;
import com.svcore.api_demo.models.dtos.DteViewDTO;

/** Nota de Crédito electrónica: devolución parcial o total sobre un Comprobante de Crédito Fiscal sellado. */
public interface CreditNoteService {

	DteViewDTO create(CreditNoteDTO info);
}
