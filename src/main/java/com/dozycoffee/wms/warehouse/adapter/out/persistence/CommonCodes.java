package com.dozycoffee.wms.warehouse.adapter.out.persistence;

final class CommonCodes {

    private CommonCodes() {
    }

    static <E extends Enum<E>> String toCode(String groupCode, E value) {
        return groupCode + "_" + value.name();
    }

    static <E extends Enum<E>> E fromCode(Class<E> type, String code) {
        String value = code.substring(code.lastIndexOf('_') + 1);
        return Enum.valueOf(type, value);
    }
}
