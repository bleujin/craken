package net.ion.craken.node.crud.tree.logging ;


/**
 * Log abstraction for the tree module. For this module, message ids
 * ranging from 1001 to 2000 inclusively have been reserved.
 *
 * @author Galder Zamarreño
 * @since 5.0
 */
// @MessageLogger(projectCode = "ISPN")
public interface Log extends org.infinispan.util.logging.Log {
}
