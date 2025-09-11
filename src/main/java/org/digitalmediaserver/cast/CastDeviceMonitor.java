/*
 * Copyright 2014 Vitaly Litvak (vitavaque@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.digitalmediaserver.cast;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;


/**
 * A class that can be used to discover cast devices and create corresponding
 * {@link CastDevice}s while notifying registered listeners of discovered or
 * disappearing devices.
 * <p>
 * After creating a {@link CastDeviceMonitor}, it must be started using
 * {@link #startDiscovery(InetAddress, String)}.
 */
public final class CastDeviceMonitor {

	private static final Logger LOGGER = LoggerFactory.getLogger(CastDeviceMonitor.class);

	/** The {@link ServiceListener} that listens to mDNS changes */
	@Nonnull
	protected final MulticastDNSServiceListener listener = new MulticastDNSServiceListener();

	/** The synchronization object */
	@Nonnull
	protected final Object lock = new Object();

	/** The mDNS instance */
	@Nullable
	@GuardedBy("lock")
	protected JmDNS mDNS;

	/** The {@link Set} of {@link DeviceDiscoveryListener} to notify of changes */
	@Nonnull
	@GuardedBy("lock")
	protected final Set<DeviceDiscoveryListener> listeners = new LinkedHashSet<>();

	/** The currently "registered" cast devices */
	@Nonnull
	@GuardedBy("lock")
	protected final Set<CastDevice> castDevices = new LinkedHashSet<>();

	/**
	 * Creates a new instance. Use {@link #startDiscovery()} to start
	 * monitoring.
	 */
	public CastDeviceMonitor() {
	}

	/**
	 * @return A {@link Set} with a snapshot of the currently known cast
	 *         devices.
	 */
	public Set<CastDevice> getCastDevices() {
		synchronized (lock) {
			return new LinkedHashSet<>(castDevices);
		}
	}

	/**
	 * Starts discovery of cast devices.
	 * <p>
	 * <b>Note:</b> This is a convenience method. The preferred constructor is
	 * {@link #startDiscovery(InetAddress, String)}. Check that your platform
	 * correctly handles the default localhost IP address and the local
	 * hostname. If in doubt, use the explicit constructor.
	 * <p>
	 * This method is equivalent to {@code startDiscovery(null, null)}.
	 *
	 * @throws IOException If an error occurs during the operation.
	 */
	public void startDiscovery() throws IOException {
		startDiscovery(null, null);
	}

	/**
	 * Starts discovery of cast devices.
	 *
	 * @param addr the IP address to bind to.
	 * @param name the name of the multicast DNS "device" that will be created
	 *            to participate in the "multicast DNS network".
	 * @throws IOException If an error occurs during the operation.
	 */
	public void startDiscovery(@Nullable InetAddress addr, @Nullable String name) throws IOException {
		synchronized (lock) {
			if (mDNS == null) {
				mDNS = JmDNS.create(addr, name);
				mDNS.addServiceListener(CastDevice.SERVICE_TYPE, listener);
			}
		}
	}

	/**
	 * Stops discovery of cast devices and removes all discovered devices.
	 *
	 * @throws IOException If an error occurs during the operation.
	 */
	public void stopDiscovery() throws IOException {
		stopDiscovery(false);
	}

	/**
	 * Stops discovery of cast devices and removes all discovered devices.
	 *
	 * @param notifyListeners if {@code true}, also notifies listeners that the
	 *            devices are removed, to trigger potential cleanup.
	 * @throws IOException If an error occurs during the operation.
	 */
	public void stopDiscovery(boolean notifyListeners) throws IOException {
		Set<CastDevice> tmpDevices = null;
		Set<DeviceDiscoveryListener> tmpListeners = null;
		synchronized (lock) {
			if (mDNS != null) {
				mDNS.close();
				mDNS = null;
			}
			if (notifyListeners) {
				tmpDevices = new LinkedHashSet<>(castDevices);
				tmpListeners = new LinkedHashSet<>(listeners);
			}
			castDevices.clear();
		}

		if (tmpDevices != null && !tmpDevices.isEmpty() && tmpListeners != null && !tmpListeners.isEmpty()) {
			for (CastDevice device : tmpDevices) {
				for (DeviceDiscoveryListener discoveryListener : tmpListeners) {
					discoveryListener.deviceRemoved(device);
				}
			}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		stopDiscovery(true);
		super.finalize();
	}

	/**
	 * Register a new {@link DeviceDiscoveryListener} that will be notified when
	 * cast devices are found or disappears. It will also be notified of any
	 * already known cast devices at the time of registration before returning.
	 *
	 * @param listener the {@link DeviceDiscoveryListener} to add.
	 * @return {@code true} if the new listener was registered, {@code false} if
	 *         it was registered already.
	 */
	public boolean registerListener(@Nullable DeviceDiscoveryListener listener) {
		if (listener == null) {
			return false;
		}
		Set<CastDevice> tmpDevices = null;
		boolean result;
		synchronized (lock) {
			result = listeners.add(listener);
			if (result) {
				tmpDevices = new LinkedHashSet<>(castDevices);
			}
		}
		if (tmpDevices != null && !tmpDevices.isEmpty()) {
			for (CastDevice tmpDevice : tmpDevices) {
				listener.deviceDiscovered(tmpDevice);
			}
		}
		return result;
	}

	/**
	 * Unregister a {@link DeviceDiscoveryListener} so that it will no longer be
	 * notified when cast devices are found or disappears.
	 *
	 * @param listener the {@link DeviceDiscoveryListener} to remove.
	 * @return {@code true} if the listener was unregistered, {@code false} if
	 *         it wasn't registered before the call.
	 */
	public boolean unregisterListener(@Nullable DeviceDiscoveryListener listener) {
		if (listener == null) {
			return false;
		}
		synchronized (lock) {
			return listeners.remove(listener);
		}
	}

	/**
	 * Service listener to receive mDNS service updates.
	 */
	public class MulticastDNSServiceListener implements ServiceListener {

		@Override
		public void serviceAdded(ServiceEvent se) {
			if (se.getDNS() != null && se.getInfo() != null) {
				ServiceInfo info = se.getDNS().getServiceInfo(CastDevice.SERVICE_TYPE, se.getInfo().getName());
				String id;
				if (info == null || Util.isBlank(id = info.getPropertyString("id"))) {
					return;
				}
				InetAddress address;
				if (info.getInet4Addresses().length > 0) {
					address = info.getInet4Addresses()[0];
				} else if (info.getInet6Addresses().length > 0) {
					address = info.getInet6Addresses()[0];
				} else {
					return;
				}
				CastDevice newDevice = null;
				Set<DeviceDiscoveryListener> tmpListeners = null;
				synchronized (lock) {
					boolean found = false;
					for (CastDevice device : castDevices) {
						if (
							id.equals(device.getUniqueId()) &&
							Objects.equals(address, device.getAddress()) &&
							info.getPort() == device.getPort()
						) {
							found = true;
							break;
						}
					}
					if (!found) {
						newDevice = new CastDevice(info, true);
						castDevices.add(newDevice);
						if (!listeners.isEmpty()) {
							tmpListeners = new LinkedHashSet<>(listeners);
						}
					}
				}
				if (newDevice != null && tmpListeners != null) {
					for (DeviceDiscoveryListener discoveryListener : tmpListeners) {
						discoveryListener.deviceDiscovered(newDevice);
					}
				}
			}
		}

		@Override
		public void serviceRemoved(ServiceEvent se) {
			ServiceInfo info = se.getInfo();
			String id, name;
			if (info == null || Util.isBlank(id = info.getPropertyString("id")) || Util.isBlank(name = info.getName())) {
				return;
			}

			CastDevice removed = null;
			Set<DeviceDiscoveryListener> tmpListeners = null;
			synchronized (lock) {
				for (CastDevice device : castDevices) {
					if (id.equals(device.getUniqueId()) && name.equals(device.getDNSName())) {
						removed = device;
						break;
					}
				}
				if (removed != null) {
					castDevices.remove(removed);
					if (!listeners.isEmpty()) {
						tmpListeners = new LinkedHashSet<>(listeners);
					}
				}
			}
			if (removed != null) {
				if (tmpListeners != null) {
					for (DeviceDiscoveryListener discoveryListener : tmpListeners) {
						discoveryListener.deviceRemoved(removed);
					}
				}
				try {
					removed.disconnect();
				} catch (IOException e) {
					LOGGER.warn(
						Channel.CAST_API_MARKER,
						"An error occurred while disconnecting from cast device {}: {}",
						removed.getDisplayName(),
						e.getMessage()
					);
					LOGGER.trace(Channel.CAST_API_MARKER, "", e);
				}
			}
		}

		@Override
		public void serviceResolved(ServiceEvent se) {
			// Already handled under "added"
		}
	}
}
