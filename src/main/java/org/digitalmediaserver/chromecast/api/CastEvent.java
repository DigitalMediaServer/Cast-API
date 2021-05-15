package org.digitalmediaserver.chromecast.api;

import static org.digitalmediaserver.chromecast.api.Util.requireNotNull;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;


//TODO: (Nad) Header + JavaDocs
/**
 * This interface describes  events.
 *
 * @param <T> the type data in the event.
 *
 * @author Nadahar
 */
public interface CastEvent<T> { //TODO: (Nad) Name + Rewrite JavaDocs++

	@Nonnull
	CastEventType getEventType();

	@Nullable
	T getData();

	@Immutable
	public static class DefaultCastEvent<T> implements CastEvent<T> {

		@Nonnull
		protected final CastEventType eventType;

		@Nullable
		protected final T data;

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
	 * @param <E> the {@link CastEvent} type.
	 * @param <T> the type of the event data object.
	 *
	 * @author Nadahar
	 */
	public interface CastEventListener<E extends CastEvent<T>, T> extends EventListener {

		/**
		 * Called whenever an {@link CastEvent} occurs.
		 *
		 * @param event the {@link CastEvent}.
		 */
		void onEvent(@Nonnull CastEvent<T> event);
	}

	@ThreadSafe
	public interface CastEventListenerList<E extends CastEvent<T>, T> {

		boolean add(@Nullable CastEventListener<E, T> listener, CastEventType... eventTypes);

		/**
		 * Adds the specified {@link CastEventListener}s to this
		 * {@link CastEventListenerList}.
		 *
		 * @param collection the {@link Collection} of
		 *            {@link CastEventListener}s.
		 * @param eventTypes the {@link CastEventType}s to subscribe to or none
		 *            to subscribe to all events.
		 * @return The number of listeners added or modified.
		 */
		int addAll(@Nullable Collection<CastEventListener<E, T>> collection, CastEventType... eventTypes);

		boolean remove(@Nullable CastEventListener<E, T> listener);

		boolean removeAll(@Nullable Collection<CastEventListener<E, T>> collection);

		boolean contains(@Nullable CastEventListener<E, T> listener);

		void clear();

		boolean isEmpty();
		int size();

		//Doc: Unmodifiable snapshot
		Iterable<CastEventListener<E, T>> listeners();

		//Doc: Unmodifiable snapshot
		Iterator<CastEventListener<E, T>> iterator();

		void fire(@Nullable E event);
	}

	@ThreadSafe
	public static class SimpleCastEventListenerList<E extends CastEvent<T>, T> implements CastEventListenerList<E, T> {

		protected final CopyOnWriteArrayList<CastEventListener<E, T>> listeners = new CopyOnWriteArrayList<>();

		@Nonnull
		protected final Object filtersLock = new Object();

		@Nonnull
		@GuardedBy("filtersLock")
		protected final Map<CastEventListener<E, T>, Set<CastEventType>> filters = new HashMap<>();

		@Override
		public boolean add(@Nullable CastEventListener<E, T> listener, CastEventType... eventTypes) {
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
		public int addAll(@Nullable Collection<CastEventListener<E, T>> collection, CastEventType... eventTypes) {
			if (collection == null || collection.isEmpty()) {
				return 0;
			}
			int result = 0;
			synchronized (filtersLock) {
				for (CastEventListener<E, T> listener : collection) {
					if (add(listener, eventTypes)) {
						result++;
					}
				}
			}
			return result;
		}

		@Override
		public boolean remove(@Nullable CastEventListener<E, T> listener) {
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
		public boolean removeAll(@Nullable Collection<CastEventListener<E, T>> collection) {
			if (collection == null || collection.isEmpty()) {
				return false;
			}
			boolean result = false;
			synchronized (filtersLock) {
				for (CastEventListener<E, T> listener : collection) {
					result |= remove(listener);
				}
			}
			return result;
		}

		@Override
		public boolean contains(@Nullable CastEventListener<E, T> listener) {
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
		public Iterable<CastEventListener<E, T>> listeners() {
			// Using Iterable instead of List avoids an extra arraycopy
			return new Iterable<CastEventListener<E, T>>() {
				@Override
				public Iterator<CastEventListener<E, T>> iterator()
				{
					return listeners.iterator();
				}
			};
		}

		@Override
		public Iterator<CastEventListener<E, T>> iterator() {
			return listeners.iterator();
		}

		@Override
		public void fire(@Nullable E event) {
			if (event == null || listeners.isEmpty()) {
				return;
			}

			Map<CastEventListener<E, T>, Set<CastEventType>> filtersSnapshot;
			synchronized (filtersLock) {
				filtersSnapshot = new HashMap<>(filters);
			}
			CastEventListener<E, T> listener;
			Set<CastEventType> targetTypes;
			E tmpEvent;
			for (Iterator<CastEventListener<E, T>> iterator = listeners.iterator(); iterator.hasNext();) {
				listener = iterator.next();
				targetTypes = filtersSnapshot.get(listener);
				if (targetTypes == null || targetTypes.contains(event.getEventType())) {
					if (event.getEventType() == CastEventType.UNKNOWN && event.getData() instanceof JsonNode) {
						// Data might be mutable, so make a copy for each listener
						tmpEvent = new DefaultCastEvent<>(CastEventType.UNKNOWN, ((JsonNode) event.getData()).deepCopy());
					} else {
						tmpEvent = event;
					}
					listener.onEvent(tmpEvent);
				}
			}
		}
	}

	@ThreadSafe
	public static class ThreadedCastEventListenerList<E extends CastEvent<T>, T> extends SimpleCastEventListenerList<E, T> { //TODO: (Nad) Remove if not used..

		private static final Logger LOGGER = LoggerFactory.getLogger(ThreadedCastEventListenerList.class);

		@Nonnull
		protected final Executor notifier;

		public ThreadedCastEventListenerList(@Nonnull Executor notifier) {
			requireNotNull(notifier, "notifier");
			this.notifier = notifier;
		}

		@Nonnull
		public Executor getExecutor() {
			return notifier;
		}

		@Override
		public void fire(@Nullable final E event) { //TODO: (Nad) Fix, include filters if keep
			if (event == null) {
				return;
			}

			final Iterator<CastEventListener<E, T>> iterator = listeners.iterator();
			try {
				notifier.execute(new Runnable() {

					@Override
					public void run() {
						while (iterator.hasNext()) {
							iterator.next().onEvent(event);
						}
					}
				});
			} catch (RejectedExecutionException e) {
				LOGGER.warn("Unable to notify listeners of event: " + e.getMessage());
				LOGGER.trace("", e);
			}
		}
	}

	public enum CastEventType {

		/** Data type will be {@link Boolean} */
		CONNECTED(Boolean.class),

		/** Data type will be {@link MediaStatus} */
		MEDIA_STATUS(MediaStatus.class),

		/** Data type will be {@link ReceiverStatus} */
		RECEIVER_STATUS(ReceiverStatus.class),

		/** Data type will be {@link CustomMessageEvent} */
		APPEVENT(CustomMessageEvent.class), //TODO: (Nad) Decide on name

		/** Special event usually received when session is stopped */
		CLOSE(Object.class), //TODO: (Nad) Figure out, shouldn't have a payload at all..?

		/** Data type will be {@link JsonNode} */
		UNKNOWN(JsonNode.class);

		private final Class<?> dataClass;

		CastEventType(Class<?> dataClass) {
			this.dataClass = dataClass;
		}

		public Class<?> getDataClass() {
			return this.dataClass;
		}
	}
}
