package dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web;

import dev.viktorstrohush.orderflow.order.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web.dto.PlaceOrderRequest;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * Request → command, written by MapStruct while compiling: the implementation is
 * {@code OrderWebMapperImpl}, in target/generated-sources. Both sides are records whose components
 * have the same names, so nothing has to be configured, not even the list of lines.
 *
 * <p>With ERROR, a component added to the command and not filled in here breaks the build.
 *
 * <p>The opposite direction ({@code OrderResponse.from}) stays written by hand: Order is not a bag
 * of getters, and that conversion calls domain logic such as {@code total()}.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
interface OrderWebMapper {

    /** The customer is not in the body: it comes from the token, as a second source. */
    PlaceOrderCommand toCommand(PlaceOrderRequest request, String customerId);
}
