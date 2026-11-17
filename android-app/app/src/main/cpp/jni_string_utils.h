#pragma once

#include <jni.h>
#include <cstdint>
#include <string>

inline std::u16string decodeUtf8ForJni(const std::string& input) {
    std::u16string output;
    output.reserve(input.size());

    for (size_t i = 0; i < input.size();) {
        const unsigned char first = static_cast<unsigned char>(input[i]);
        uint32_t codePoint = first;
        size_t length = 1;
        bool valid = true;

        if (first <= 0x7F) {
            codePoint = first;
        } else if (first >= 0xC2 && first <= 0xDF && i + 1 < input.size()) {
            const unsigned char second = static_cast<unsigned char>(input[i + 1]);
            valid = (second & 0xC0) == 0x80;
            if (valid) {
                codePoint = ((first & 0x1F) << 6) | (second & 0x3F);
                length = 2;
            }
        } else if (first >= 0xE0 && first <= 0xEF && i + 2 < input.size()) {
            const unsigned char second = static_cast<unsigned char>(input[i + 1]);
            const unsigned char third = static_cast<unsigned char>(input[i + 2]);
            valid = (second & 0xC0) == 0x80 && (third & 0xC0) == 0x80
                    && !(first == 0xE0 && second < 0xA0)
                    && !(first == 0xED && second >= 0xA0);
            if (valid) {
                codePoint = ((first & 0x0F) << 12)
                        | ((second & 0x3F) << 6) | (third & 0x3F);
                length = 3;
            }
        } else if (first >= 0xF0 && first <= 0xF4 && i + 3 < input.size()) {
            const unsigned char second = static_cast<unsigned char>(input[i + 1]);
            const unsigned char third = static_cast<unsigned char>(input[i + 2]);
            const unsigned char fourth = static_cast<unsigned char>(input[i + 3]);
            valid = (second & 0xC0) == 0x80
                    && (third & 0xC0) == 0x80
                    && (fourth & 0xC0) == 0x80
                    && !(first == 0xF0 && second < 0x90)
                    && !(first == 0xF4 && second > 0x8F);
            if (valid) {
                codePoint = ((first & 0x07) << 18)
                        | ((second & 0x3F) << 12)
                        | ((third & 0x3F) << 6)
                        | (fourth & 0x3F);
                length = 4;
            }
        } else {
            valid = false;
        }

        // SF2 files sometimes contain legacy single-byte names. If a byte
        // sequence is not valid UTF-8, preserve that byte as a Latin-1 code point.
        if (!valid) {
            codePoint = first;
            length = 1;
        }

        if (codePoint <= 0xFFFF) {
            output.push_back(static_cast<char16_t>(codePoint));
        } else {
            codePoint -= 0x10000;
            output.push_back(static_cast<char16_t>(0xD800 + (codePoint >> 10)));
            output.push_back(static_cast<char16_t>(0xDC00 + (codePoint & 0x3FF)));
        }

        i += length;
    }

    return output;
}

inline jstring newJStringFromUtf8(JNIEnv* env, const std::string& value) {
    if (!env) return nullptr;
    const std::u16string decoded = decodeUtf8ForJni(value);
    return env->NewString(
            reinterpret_cast<const jchar*>(decoded.data()),
            static_cast<jsize>(decoded.size()));
}
