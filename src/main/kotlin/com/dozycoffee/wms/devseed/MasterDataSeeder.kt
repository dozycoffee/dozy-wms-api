package com.dozycoffee.wms.devseed

import com.dozycoffee.wms.product.application.port.`in`.DeactivateProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.DeleteProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.RegisterProductUseCase
import com.dozycoffee.wms.product.application.port.`in`.command.RegisterProductCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterLocationUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterZoneUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterLocationCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterWarehouseCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.RegisterZoneCommand
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
internal class MasterDataSeeder(
    private val registerWarehouseUseCase: RegisterWarehouseUseCase,
    private val registerZoneUseCase: RegisterZoneUseCase,
    private val registerLocationUseCase: RegisterLocationUseCase,
    private val registerWorkAreaUseCase: RegisterWorkAreaUseCase,
    private val registerProductUseCase: RegisterProductUseCase,
    private val deactivateProductUseCase: DeactivateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase
) {

    suspend fun seed(): SeedContext {
        val warehouse = registerWarehouseUseCase.register(
            RegisterWarehouseCommand(
                SeedData.WAREHOUSE_NAME,
                SeedData.WAREHOUSE_ADDRESS,
                BigDecimal("37.279200"),
                BigDecimal("127.442500")
            )
        ).awaitSingle()
        val warehouseId: Long = warehouse.warehouseId

        AreaCode.entries.forEach { areaCode ->
            registerWorkAreaUseCase.register(RegisterWorkAreaCommand(warehouseId, areaCode)).awaitSingle()
        }

        val zoneIdByCode: Map<ZoneCode, Long> = ZoneCode.entries.associateWith { zoneCode ->
            registerZoneUseCase.register(RegisterZoneCommand(warehouseId, zoneCode)).awaitSingle().zoneId
        }

        val locationIdByCode: Map<String, Long> = SeedData.LOCATIONS.associate { spec ->
            val zoneId: Long = zoneIdByCode.getValue(spec.zoneCode)
            val location = registerLocationUseCase.register(
                RegisterLocationCommand(zoneId, spec.locationCode, spec.maxCapacity)
            ).awaitSingle()
            spec.locationCode to location.locationId
        }

        val productIdByCode: Map<String, Long> = SeedData.PRODUCTS.associate { spec ->
            spec.productCode to registerProduct(spec)
        }

        val inactiveProductId: Long = registerProduct(SeedData.INACTIVE_PRODUCT)
        deactivateProductUseCase.deactivate(inactiveProductId)
        val deletedProductId: Long = registerProduct(SeedData.DELETED_PRODUCT)
        deleteProductUseCase.delete(deletedProductId)

        return SeedContext(warehouseId, zoneIdByCode, locationIdByCode, productIdByCode)
    }

    private suspend fun registerProduct(spec: ProductSpec): Long {
        return registerProductUseCase.register(
            RegisterProductCommand(spec.productCode, spec.productName, spec.category, spec.unit, spec.shelfLifeDays)
        ).productId
    }
}
