package ca.bc.gov.open.digitalformsapi.viirp.service;

import org.springframework.stereotype.Service;

import ca.bc.gov.open.digitalformsapi.viirp.exception.ConflictException;
import ca.bc.gov.open.digitalformsapi.viirp.exception.DigitalFormsException;
import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

@Service
public class NoticeConflictService {

    private final VipsRestService vips;

    public NoticeConflictService(VipsRestService vips) {
        this.vips = vips;
    }

    public void checkProhibition(String noticeNo) {
        var response = vips.searchProhibition(noticeNo);
        if (response == null || response.getRespCd() != DigitalFormsConstants.VIPSWS_SUCCESS_CD
                || response.getResult() == null) {
            throw new DigitalFormsException("Unable to check prohibition notice availability.");
        }
        if (response.getResult().stream().anyMatch(row -> noticeNo.equals(row.getProhibitionNoticeNo()))) {
            throw new ConflictException("Prohibition notice " + noticeNo + " already exists.");
        }
    }

    public void checkImpoundment(String noticeNo) {
        var response = vips.searchImpoundment(noticeNo);
        if (response == null || response.getRespCd() != DigitalFormsConstants.VIPSWS_SUCCESS_CD
                || response.getResult() == null) {
            throw new DigitalFormsException("Unable to check impoundment notice availability.");
        }
        if (response.getResult().stream().anyMatch(row -> noticeNo.equals(row.getImpoundmentNoticeNo()))) {
            throw new ConflictException("Impoundment notice " + noticeNo + " already exists.");
        }
    }
}
