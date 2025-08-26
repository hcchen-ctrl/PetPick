// src/main/java/com/xxx/order/ShippingValidators.java
package com.petpick.petpick.Utils;

import java.util.regex.Pattern;

public final class ShippingValidators {
  private ShippingValidators() {}

  // 允許：中英數、空白、- # () 全形() 、，。 . / 與全形斜線
  private static final Pattern HOME_ADDR_ALLOWED = Pattern.compile(
      "^[\\p{IsHan}A-Za-z0-9\\s\\-#()（） 、，。./／]+$"
  );
  // 禁 emoji / 擴展圖形符號
  private static final Pattern EMOJI = Pattern.compile("[\\x{1F300}-\\x{1FAFF}]");

  public static String normalize(String s) {
    if (s == null) return "";
    return s.replaceAll("\\s+", " ").trim();
  }

  public static void validateHomeAddress(String addr) {
    String a = normalize(addr);
    if (a.length() < 6 || a.length() > 60) {
      throw bad("請填寫正確收件地址（6–60 字）");
    }
    if (EMOJI.matcher(a).find()) {
      throw bad("收件地址不可包含表情符號或特殊符號");
    }
    if (!HOME_ADDR_ALLOWED.matcher(a).matches()) {
      throw bad("地址含不支援字元（僅限中英數、空白、- # () 、，。 . /）");
    }
  }

  public static void validateZipNullable(String zip) {
    if (zip == null || zip.isBlank()) return;
    if (!zip.matches("^\\d{3,5}$")) throw bad("郵遞區號格式不正確");
  }

  public static void validatePhone09(String phone) {
    if (!String.valueOf(phone).matches("^09\\d{8}$")) throw bad("請輸入正確手機號碼");
  }

  private static IllegalArgumentException bad(String msg) {
    return new IllegalArgumentException(msg);
  }
}