package com.mr.domain.auth.converter;

import com.mr.domain.auth.entity.enums.SocialType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class SocialTypeConverter implements Converter<String, SocialType> {

    @Override
    public SocialType convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return SocialType.from(source);
    }
}
