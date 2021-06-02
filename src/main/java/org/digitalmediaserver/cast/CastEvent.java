/*
 * Copyright (C) 2021 Digital Media Server developers.
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

import static org.digitalmediaserver.cast.Util.requireNotBlank;
import static org.digitalmediaserver.cast.Util.requireNotNull;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import org.digitalmediaserver.cast.StandardResponse.AppAvailabilityResponse;
import org.digitalmediaserver.cast.StandardResponse.CloseResponse;
import org.digitalmediaserver.cast.StandardResponse.DeviceAddedResponse;
import org.digitalmediaserver.cast.StandardResponse.DeviceRemovedResponse;
import org.digitalmediaserver.cast.StandardResponse.DeviceUpdatedResponse;
import org.digitalmediaserver.cast.StandardResponse.ErrorResponse;
import org.digitalmediaserver.cast.StandardResponse.LaunchErrorResponse;
import org.digitalmediaserver.cast.StandardResponse.MediaStatusResponse;
import org.digitalmediaserver.cast.StandardResponse.MultizoneStatusResponse;
import org.digitalmediaserver.cast.StandardResponse.ReceiverStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;


/**
 * This interface describes cast device related events.
 *
 * @param <T> the type data in the event.
 *
 * @author Nadahar
 */
@Immutable
public interface CastEvent<T> {

	/**
	 * @return The {@link CastEventType} of this event.
	 */
	@Nonnull
	CastEventType getEventType();

	/**
	 * @return The event data, if any.
	 */
	@Nullable
	T getData();

	/**
	 * Tries to cast the event data to the specified type and return it.
	 *
	 * @param <U> the event data type.
	 * @param cls the {@link Class} of the event data type.
	 * @return The event data cast to the specified type or {@code null}.
	 */
	@Nullable
	<U> U getData(Class<U> cls);

	/**
	 * The default {@link CastEvent} implementation.
	 *
	 * @param <T> the type data in the event.
	 *
	 * @author Nadahar
	 */
	@Immutable
	public static class DefaultCastEvent<T> implements CastEvent<T> {

		/** The {@link CastEventType} */
		@Nonnull
		protected final CastEventType eventType;

		/** The event data */
		@Nullable
		protected final T data;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param eventType the {@link CastEventType}.
		 * @param data the event data or {@code null}.
		 * @throws IllegalArgumentException If {@code eventType} is
		 *             {@code null}.
		 */
		public DefaultCastEvent(@Nonnull CastEventType eventType, @Nullable T data) {
			requireNotNull(eventType, "eventType");
			this.eventType = eventType;
			this.data = data;
		}

		@Override
		@Nonnull
		public CastEventType getEventType() {
			return eventType;
		}

		@Override
		@Nullable
		public T getData() {
			return data;
		}

		@Override
		@Nullable
		public <U> U getData(Class<U> cls) {
			if (data == null) {
				return null;
			}
			if (!cls.isAssignableFrom(eventType.getDataClass())) {
				throw new IllegalArgumentException(
					"Requested type " + cls + " does not match type for event " + eventType.getDataClass()
				);
			}
			return cls.cast(data);
		}

		@Override
		public String toString() {
			return new StringBuilder(50).append(getClass().getSimpleName())
				.append(" [Type: ").append(eventType)
				.append(", Data: ").append(data).append(']')
				.toString();
		}
	}

	/**
	 * An {@link EventListener} that listens for {@link CastEvent}s.
	 *
	 * @author Nadahar
	 */
	public interface CastEventListener extends EventListener {

		/**
		 * Called whenever an {@link CastEvent} occurs.
		 *
		 * @param event the {@link CastEvent}.
		 */
		void onEvent(@Nonnull CastEvent<?> event);
	}

	/**
	 * A thread-safe {@link List} of {@link CastEventListener}s.
	 *
	 * @author Nadahar
	 */
	@ThreadSafe
	public interface CastEventListenerList {

		/**
		 * Registers the specified {@link CastEventListener} to this
		 * {@link CastEventListenerList} for the specified
		 * {@link CastEventType}s.
		 *
		 * @param listener the {@link CastEventListener} to register.
		 * @param eventTypes the event type(s) to listen to.
		 * @return {@code true} if a change was made to the listener list,
		 *         {@code false} if this registration deadn't lead to any
		 *         change.
		 */
		boolean add(@Nullable CastEventListener listener, CastEventType... eventTypes);

		/**
		 * Registers the specified {@link CastEventListener}s to this
		 * {@link CastEventListenerList} for the specified
		 * {@link CastEventType}s.
		 *
		 * @param collection the {@link Collection} of
		 *            {@link CastEventListener}s.
		 * @param eventTypes the {@link CastEventType}s to subscribe to or none
		 *            to subscribe to all events.
		 * @return The number of listeners added or modified.
		 */
		int addAll(@Nullable Collection<CastEventListener> collection, CastEventType... eventTypes);

		/**
		 * Unregisters the specified {@link CastEventListener} from this
		 * {@link CastEventListenerList}.
		 *
		 * @param listener the {@link CastEventListener} to unregister.
		 * @return {@code true} if a change was made to the listener list,
		 *         {@code false} if this registration deadn't lead to any
		 *         change.
		 */
		boolean remove(@Nullable CastEventListener listener);

		/**
		 * Unregisters the specified {@link CastEventListener}s from this
		 * {@link CastEventListenerList}.
		 *
		 * @param collection the {@link Collection} of
		 *            {@link CastEventListener}s.
		 * @return {@code true} if a change was made to the listener list,
		 *         {@code false} if this registration deadn't lead to any
		 *         change.
		 */
		boolean removeAll(@Nullable Collection<CastEventListener> collection);

		/**
		 * Checks if the specified {@link CastEventListener} is already
		 * registered with this {@link CastEventListenerList}.
		 *
		 * @param listener the {@link CastEventListener} to check.
		 * @return {@code true} if the specified {@link CastEventListener} is
		 *         registered, {@code false} otherwise.
		 */
		boolean contains(@Nullable CastEventListener listener);

		/**
		 * Unregisters all {@link CastEventListener}s from this
		 * {@link CastEventListenerList}.
		 */
		void clear();

		/**
		 * @return {@code true} if this {@link CastEventListenerList} has no
		 *         registered {@link CastEventListener}s, {@code false}
		 *         otherwise.
		 */
		boolean isEmpty();

		/**
		 * @return The number of registered {@link CastEventListener}s.
		 */
		int size();

		/**
		 * @return An unmodifiable snapshot {@link Iterable} captured at the
		 *         time this method is called.
		 */
		Iterable<CastEventListener> listeners();

		/**
		 * @return An unmodifiable snapshot {@link Iterator} captured at the
		 *         time this method is called.
		 */
		Iterator<CastEventListener> iterator();

		/**
		 * Fires the specified event on all the registered
		 * {@link CastEventListener}s.
		 *
		 * @param event the {@link CastEvent} to fire.
		 */
		void fire(@Nullable CastEvent<?> event);
	}

	/**
	 * An abstract {@link CastEventListenerList} implementation that implements
	 * every method except {@link CastEventListenerList#fire(CastEvent)}.
	 *
	 * @author Nadahar
	 */
	public abstract static class AbstractCastEventListenerList implements CastEventListenerList {

		/** The identifier for the cast device used in logging */
		@Nonnull
		protected final String remoteName;

		/** The {@link List} containing the listeners */
		protected final CopyOnWriteArrayList<CastEventListener> listeners = new CopyOnWriteArrayList<>();

		/** The lock object for the filters */
		@Nonnull
		protected final Object filtersLock = new Object();

		/**
		 * The filters that keep track of what {@link CastEventType}s to send to
		 * which {@link CastEventListener}s
		 */
		@Nonnull
		@GuardedBy("filtersLock")
		protected final Map<CastEventListener, Set<CastEventType>> filters = new HashMap<>();

		/**
		 * Abstract constructor that sets the "remote name" final field.
		 *
		 * @param remoteName the identifier for the cast device used in logging.
		 * @throws IllegalArgumentException If {@code remoteName} is
		 *             {@code null}.
		 */
		protected AbstractCastEventListenerList(@Nonnull String remoteName) {
			requireNotBlank(remoteName, "remoteName");
			this.remoteName = remoteName;
		}

		@Override
		public boolean add(@Nullable CastEventListener listener, CastEventType... eventTypes) {
			if (listener == null) {
				return false;
			}
			synchronized (filtersLock) {
				if (listeners.contains(listener)) {
					if (eventTypes == null || eventTypes.length == 0) {
						return filters.remove(listener) != null;
					}
					EnumSet<CastEventType> newTypes = EnumSet.copyOf(Arrays.asList(eventTypes));
					Set<CastEventType> types = filters.get(listener);
					if (types == null) {
						filters.put(listener, newTypes);
						return true;
					}
					return types.addAll(newTypes);
				}

				listeners.add(listener);
				if (eventTypes != null && eventTypes.length > 0) {
					filters.put(listener, EnumSet.copyOf(Arrays.asList(eventTypes)));
				}
				return true;
			}
		}

		@Override
		public int addAll(@Nullable Collection<CastEventListener> collection, CastEventType... eventTypes) {
			if (collection == null || collection.isEmpty()) {
				return 0;
			}
			int result = 0;
			synchronized (filtersLock) {
				for (CastEventListener listener : collection) {
					if (add(listener, eventTypes)) {
						result++;
					}
				}
			}
			return result;
		}

		@Override
		public boolean remove(@Nullable CastEventListener listener) {
			if (listener == null) {
				return false;
			}
			if (listeners.remove(listener)) {
				synchronized (filtersLock) {
					filters.remove(listener);
				}
				return true;
			}
			return false;
		}

		@Override
		public boolean removeAll(@Nullable Collection<CastEventListener> collection) {
			if (collection == null || collection.isEmpty()) {
				return false;
			}
			boolean result = false;
			synchronized (filtersLock) {
				for (CastEventListener listener : collection) {
					result |= remove(listener);
				}
			}
			return result;
		}

		@Override
		public boolean contains(@Nullable CastEventListener listener) {
			return listener == null ? false : listeners.contains(listener);
		}

		@Override
		public void clear() {
			listeners.clear();
			synchronized (filtersLock) {
				filters.clear();
			}
		}

		@Override
		public boolean isEmpty() {
			return listeners.isEmpty();
		}

		@Override
		public int size() {
			return listeners.size();
		}

		@Override
		public Iterable<CastEventListener> listeners() {
			// Using Iterable instead of List avoids an extra arraycopy
			return new Iterable<CastEventListener>() {
				@Override
				public Iterator<CastEventListener> iterator() {
					return listeners.iterator();
				}
			};
		}

		@Override
		public Iterator<CastEventListener> iterator() {
			return listeners.iterator();
		}
	}

	/**
	 * A simple (as in not threaded) {@link CastEventListenerList}
	 * implementation that invokes {@link CastEventListener}s from the thread
	 * that calls {@link #fire(CastEvent)}.
	 *
	 * @author Nadahar
	 */
	@ThreadSafe
	public static class SimpleCastEventListenerList extends AbstractCastEventListenerList {

		private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCastEventListenerList.class);

		/**
		 * Creates a new instance with the specified remote name.
		 *
		 * @param remoteName the identifier for the cast device used in logging.
		 * @throws IllegalArgumentException If {@code remoteName} is
		 *             {@code null}.
		 */
		public SimpleCastEventListenerList(@Nonnull String remoteName) {
			super(remoteName);
		}

		@Override
		public void fire(@Nullable CastEvent<?> event) {
			if (event == null) {
				return;
			}

			if (listeners.isEmpty()) {
				if (LOGGER.isDebugEnabled(Channel.CAST_API_MARKER)) {
					LOGGER.debug(
						Channel.CAST_API_MARKER,
						"No cast event listener, but would have notified them of the following event from {}: {}",
						remoteName,
						event
					);
				}
				return;
			}
			if (LOGGER.isDebugEnabled(Channel.CAST_API_MARKER)) {
				LOGGER.debug(
					Channel.CAST_API_MARKER,
					"Notifying cast event listeners of the following event from {}: {}",
					remoteName,
					event
				);
			}

			Map<CastEventListener, Set<CastEventType>> filtersSnapshot;
			synchronized (filtersLock) {
				filtersSnapshot = new HashMap<>(filters);
			}
			CastEventListener listener;
			Set<CastEventType> targetTypes;
			for (Iterator<CastEventListener> iterator = listeners.iterator(); iterator.hasNext();) {
				listener = iterator.next();
				targetTypes = filtersSnapshot.get(listener);
				if (targetTypes == null || targetTypes.contains(event.getEventType())) {
					if (event.getEventType() == CastEventType.UNKNOWN && event.getData() instanceof JsonNode) {
						// Data might be mutable, so make a copy for each listener
						listener.onEvent(new DefaultCastEvent<>(CastEventType.UNKNOWN, ((JsonNode) event.getData()).deepCopy()));
					} else {
						listener.onEvent(event);
					}
				}
			}
		}
	}

	/**
	 * A threaded {@link CastEventListenerList} implementation that invokes
	 * {@link CastEventListener}s using the specified {@link Executor} when
	 * {@link #fire(CastEvent)} is called.
	 *
	 * @author Nadahar
	 */
	@ThreadSafe
	public static class ThreadedCastEventListenerList extends AbstractCastEventListenerList {

		private static final Logger LOGGER = LoggerFactory.getLogger(ThreadedCastEventListenerList.class);

		/** The {@link Executor} that is used to invoke listeners */
		@Nonnull
		protected final Executor notifier;

		/**
		 * Creates a new instance with the specified notifier and remote name.
		 *
		 * @param notifier the {@link Executor} that will invoke the listeners.
		 * @param remoteName the identifier for the cast device used in logging.
		 * @throws IllegalArgumentException If {@code notifier} or
		 *             {@code remoteName} is {@code null}.
		 */
		public ThreadedCastEventListenerList(@Nonnull Executor notifier, @Nonnull String remoteName) {
			super(remoteName);
			requireNotNull(notifier, "notifier");
			this.notifier = notifier;
		}

		/**
		 * @return The {@link Executor} that notifies listeners.
		 */
		@Nonnull
		public Executor getNotifier() {
			return notifier;
		}

		@Override
		public void fire(@Nullable CastEvent<?> event) {
			if (event == null) {
				return;
			}

			if (listeners.isEmpty()) {
				if (LOGGER.isDebugEnabled(Channel.CAST_API_MARKER)) {
					LOGGER.debug(
						Channel.CAST_API_MARKER,
						"No cast event listener, but would have notified them of the following event from {}: {}",
						remoteName,
						event
					);
				}
				return;
			}
			if (LOGGER.isDebugEnabled(Channel.CAST_API_MARKER)) {
				LOGGER.debug(
					Channel.CAST_API_MARKER,
					"Notifying cast event listeners of the following event from {}: {}",
					remoteName,
					event
				);
			}

			Map<CastEventListener, Set<CastEventType>> filtersSnapshot;
			synchronized (filtersLock) {
				filtersSnapshot = new HashMap<>(filters);
			}

			try {
				CastEventListener listener;
				Set<CastEventType> targetTypes;
				Invoker invoker;
				for (Iterator<CastEventListener> iterator = listeners.iterator(); iterator.hasNext();) {
					listener = iterator.next();
					targetTypes = filtersSnapshot.get(listener);
					if (targetTypes == null || targetTypes.contains(event.getEventType())) {
						if (event.getEventType() == CastEventType.UNKNOWN && event.getData() instanceof JsonNode) {
							// Data might be mutable, so make a copy for each listener
							invoker = new Invoker(
								listener,
								new DefaultCastEvent<>(CastEventType.UNKNOWN, ((JsonNode) event.getData()).deepCopy())
							);
						} else {
							invoker = new Invoker(listener, event);
						}
						notifier.execute(invoker);
					}
				}
			} catch (RejectedExecutionException e) {
				LOGGER.warn(
					Channel.CAST_API_MARKER,
					"Unable to notify listeners of change event: " + e.getMessage()
				);
				LOGGER.trace(Channel.CAST_API_MARKER, "", e);
			}
		}

		/**
		 * A {@link Runnable} implementation that simply invokes a listener.
		 */
		protected static class Invoker implements Runnable {

			/** The {@link CastEventListener} to invoke */
			@Nonnull
			protected final CastEventListener listener;

			/** The {@link CastEvent} to deliver */
			@Nonnull
			protected final CastEvent<?> event;

			/**
			 * Creates a new instance using the specified parameters.
			 *
			 * @param listener the {@link CastEventListener} to invoke.
			 * @param event the {@link CastEvent} to deliver.
			 */
			public Invoker(@Nonnull CastEventListener listener, @Nonnull CastEvent<?> event) {
				this.listener = listener;
				this.event = event;
			}

			@Override
			public void run() {
				listener.onEvent(event);
			}
		}
	}

	/**
	 * All the {@link CastEvent} types.
	 *
	 * @author Nadahar
	 */
	public enum CastEventType {

		/**
		 * Event is fired when an unclaimed {@link AppAvailabilityResponse} is
		 * received, which shouldn't normally happen
		 */
		APPLICATION_AVAILABILITY(AppAvailabilityResponse.class),

		/**
		 * Event is fired when an unclaimed {@link CloseResponse} is received,
		 * which shouldn't normally happen
		 */
		CLOSE(CloseMessageEvent.class),

		/**
		 * Event is fired when the connection state with the cast device changes
		 */
		CONNECTED(Boolean.class),

		/** Event is fired when an unclaimed custom message is received */
		CUSTOM_MESSAGE(CustomMessageEvent.class),

		/**
		 * Event is fired when an unclaimed {@link DeviceAddedResponse} is
		 * received
		 */
		DEVICE_ADDED(DeviceAddedResponse.class),

		/**
		 * Event is fired when an unclaimed {@link DeviceRemovedResponse} is
		 * received
		 */
		DEVICE_REMOVED(DeviceRemovedResponse.class),

		/**
		 * Event is fired when an unclaimed {@link DeviceUpdatedResponse} is
		 * received
		 */
		DEVICE_UPDATED(DeviceUpdatedResponse.class),

		/**
		 * Event is fired when an unclaimed {@link ErrorResponse} is received,
		 * which shouldn't normally happen
		 */
		ERROR_RESPONSE(ErrorResponse.class),

		/**
		 * Event is fired when an unclaimed {@link LaunchErrorResponse} is
		 * received, which shouldn't normally happen
		 */
		LAUNCH_ERROR(LaunchErrorResponse.class),

		/**
		 * Event is fired when an unclaimed {@link MediaStatusResponse} is
		 * received
		 */
		MEDIA_STATUS(MediaStatusResponse.class),

		/**
		 * Event is fired when an unclaimed {@link MultizoneStatusResponse} is
		 * received
		 */
		MULTIZONE_STATUS(MultizoneStatusResponse.class),

		/**
		 * Event is fired when an unclaimed {@link ReceiverStatusResponse} is
		 * received
		 */
		RECEIVER_STATUS(ReceiverStatusResponse.class),

		/**
		 * Event is fired when a response that can't be deserialized is
		 * received, the data will be {@link JsonNode}
		 */
		UNKNOWN(JsonNode.class);

		@Nullable
		private final Class<?> dataClass;

		private CastEventType(Class<?> dataClass) {
			this.dataClass = dataClass;
		}

		/**
		 * @return The data {@link Class} for this {@link CastEventType}.
		 */
		@Nullable
		public Class<?> getDataClass() {
			return dataClass;
		}
	}
}
