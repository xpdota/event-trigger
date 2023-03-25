package gg.xp.services;

import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.ObservableSetting;
import gg.xp.xivsupport.persistence.settings.Resettable;
import gg.xp.xivsupport.persistence.settings.StringSetting;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class ServiceSelector extends ObservableSetting implements Resettable {

	private static final Logger log = LoggerFactory.getLogger(ServiceSelector.class);
	private final Object lock = new Object();
	private final List<ServiceHandle> providers = new ArrayList<>();
	private volatile ServiceHandle current;
	private final StringSetting setting;
	private static final String DEFAULT_MARKER = "";

	protected ServiceSelector(StringSetting setting) {
		this.setting = setting;
		List<ServiceHandle> defaultOptions = defaultOptions();
		if (defaultOptions.isEmpty()) {
			throw new IllegalArgumentException("Must have a default option!");
		}
		providers.addAll(defaultOptions);
		setting.addAndRunListener(this::recalc);
	}

	protected ServiceSelector(PersistenceProvider pers, String propStub) {
		this(new StringSetting(pers, propStub + ".choice", DEFAULT_MARKER));
	}

	protected abstract String name();

	protected List<ServiceHandle> defaultOptions() {
		return List.of(makeHandle(new ServiceDescriptor() {
			@Override
			public String name() {
				return "None/Other";
			}

			@Override
			public String id() {
				return "none";
			}

			@Override
			public int priority() {
				return defaultOptionPriority();
			}
		}));
	}

	/**
	 * What priority the built-in default option should have.
	 * <p>
	 * Can be safely ignored if supplying custom defaults.
	 *
	 * @return The priority for the default "none/other" option.
	 */
	protected int defaultOptionPriority() {
		return 10;
	};

	private void recalc() {
		synchronized (lock) {
			providers.sort(Comparator.comparing(provider -> -provider.descriptor().priority()));
			current = getEffectiveOption();
		}
		notifyListeners();
	}

	public @Nullable ServiceHandle getExplicitlySelectedOption() {
		synchronized (lock) {
			String value = setting.get();
			if (value == null || value.isBlank()) {
				return null;
			}
			return providers.stream().filter(provider -> provider.descriptor().id().equals(value))
					.findFirst()
					.orElse(null);
		}
	}

	public void setCurrent(@Nullable ServiceHandle serviceHandle) {
		if (serviceHandle == null) {
			log.info("{}: Service Provider De-Selected", name());
		}
		else {
			log.info("{}: Service Provider Selected: ({}) {}", name(), serviceHandle.descriptor().id(), serviceHandle.descriptor().name());
		}
		setting.set(serviceHandle == null ? DEFAULT_MARKER : serviceHandle.descriptor().id());
	}

	public List<ServiceHandle> getOptions() {
		synchronized (lock) {
			return new ArrayList<>(providers);
		}
	}

	public ServiceHandle register(ServiceDescriptor service) {
		ServiceHandle handle = makeHandle(service);
		synchronized (lock) {
			log.info("{}: Service Provider Registered: ({}) {}", name(), service.id(), service.name());
			providers.add(handle);
			recalc();
		}
		return handle;
	}

	protected ServiceHandle makeHandle(ServiceDescriptor service) {
		return new ServiceHandle() {
			@Override
			public boolean enabled() {
				return current == this;
			}

			@Override
			public void setEnabled() {
				setCurrent(this);
			}

			@Override
			public ServiceDescriptor descriptor() {
				return service;
			}
		};
	}


	public ServiceHandle defaultOption() {
		synchronized (lock) {
			recalc();
			return providers.get(0);
		}
	}

	@Override
	public void delete() {
		setCurrent(null);
	}

	@Override
	public boolean isSet() {
		return getExplicitlySelectedOption() != null;
	}

	public ServiceHandle getEffectiveOption() {
		ServiceHandle selected = getExplicitlySelectedOption();
		if (selected == null) {
			return providers.get(0);
		}
		else {
			return selected;
		}
	}
}
