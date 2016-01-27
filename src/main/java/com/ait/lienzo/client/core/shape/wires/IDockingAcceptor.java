package com.ait.lienzo.client.core.shape.wires;

public interface IDockingAcceptor {

    IDockingAcceptor DEFAULT = new DefaultDockingAcceptor();

    boolean dockingAccepted(WiresShape parent, WiresShape child);

    boolean acceptDocking(WiresShape parent, WiresShape child);

    class DefaultDockingAcceptor implements IDockingAcceptor {
        @Override
        public boolean dockingAccepted(WiresShape parent, WiresShape child) {
            return true;
        }

        @Override
        public boolean acceptDocking(WiresShape parent, WiresShape child) {
            return true;
        }
    }

}
