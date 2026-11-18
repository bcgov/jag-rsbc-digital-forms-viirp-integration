package ca.bc.gov.open.digitalformsapi.viirp.config;

import java.beans.PropertyEditorSupport;
import java.util.function.Function;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import ca.bc.gov.open.digitalformsapi.viirp.api.DocumentsApiController;
import ca.bc.gov.open.digitalformsapi.viirp.model.UploadNoticeSubject;
import ca.bc.gov.open.digitalformsapi.viirp.model.UploadNoticeType;

@ControllerAdvice(assignableTypes = DocumentsApiController.class)
public class MultipartEnumBinding {

    @InitBinder
    void bindUploadCodes(WebDataBinder binder) {
        // An editor avoids Spring's trimmed-value/empty-to-null fallback after a converter rejects an enum.
        binder.registerCustomEditor(UploadNoticeType.class, strictEnum(UploadNoticeType::fromValue));
        binder.registerCustomEditor(UploadNoticeSubject.class, strictEnum(UploadNoticeSubject::fromValue));
    }

    private static <E extends Enum<E>> PropertyEditorSupport strictEnum(Function<String, E> parse) {
        return new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(parse.apply(text));
            }
        };
    }
}
