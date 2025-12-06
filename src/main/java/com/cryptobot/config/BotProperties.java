package com.cryptobot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "")
public class BotProperties {

    private CoinGecko coingecko = new CoinGecko();
    private Bitcoin bitcoin = new Bitcoin();
    private WhatsApp whatsapp = new WhatsApp();
    private Notification notification = new Notification();

    // Getters y Setters
    public CoinGecko getCoingecko() { return coingecko; }
    public void setCoingecko(CoinGecko coingecko) { this.coingecko = coingecko; }
    public Bitcoin getBitcoin() { return bitcoin; }
    public void setBitcoin(Bitcoin bitcoin) { this.bitcoin = bitcoin; }
    public WhatsApp getWhatsapp() { return whatsapp; }
    public void setWhatsapp(WhatsApp whatsapp) { this.whatsapp = whatsapp; }
    public Notification getNotification() { return notification; }
    public void setNotification(Notification notification) { this.notification = notification; }

    // ===================================
    // COINGECKO
    // ===================================
    public static class CoinGecko {
        private Api api = new Api();
        private Cache cache = new Cache();
        private RateLimit rateLimit = new RateLimit();
        private Retry retry = new Retry();

        public Api getApi() { return api; }
        public void setApi(Api api) { this.api = api; }
        public Cache getCache() { return cache; }
        public void setCache(Cache cache) { this.cache = cache; }
        public RateLimit getRateLimit() { return rateLimit; }
        public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }
        public Retry getRetry() { return retry; }
        public void setRetry(Retry retry) { this.retry = retry; }

        public static class Api {
            private String url = "https://api.coingecko.com/api/v3/coins/bitcoin/market_chart?vs_currency=usd&days=2";
            public String getUrl() { return url; }
            public void setUrl(String url) { this.url = url; }
        }

        public static class Cache {
            private int ttlSeconds = 600;
            public int getTtlSeconds() { return ttlSeconds; }
            public void setTtlSeconds(int ttlSeconds) { this.ttlSeconds = ttlSeconds; }
        }

        public static class RateLimit {
            private int cooldownSeconds = 120;
            public int getCooldownSeconds() { return cooldownSeconds; }
            public void setCooldownSeconds(int cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }
        }

        public static class Retry {
            private int maxAttempts = 3;
            private List<Integer> delays = List.of(2000, 5000, 10000);
            public int getMaxAttempts() { return maxAttempts; }
            public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
            public List<Integer> getDelays() { return delays; }
            public void setDelays(List<Integer> delays) { this.delays = delays; }
        }
    }

    // ===================================
    // BITCOIN
    // ===================================
    public static class Bitcoin {
        private Price price = new Price();
        private Rsi rsi = new Rsi();
        private Scheduler scheduler = new Scheduler();

        public Price getPrice() { return price; }
        public void setPrice(Price price) { this.price = price; }
        public Rsi getRsi() { return rsi; }
        public void setRsi(Rsi rsi) { this.rsi = rsi; }
        public Scheduler getScheduler() { return scheduler; }
        public void setScheduler(Scheduler scheduler) { this.scheduler = scheduler; }

        public static class Price {
            private double changeThreshold = 1.0;
            private int recentPricesLimit = 20;
            public double getChangeThreshold() { return changeThreshold; }
            public void setChangeThreshold(double changeThreshold) { this.changeThreshold = changeThreshold; }
            public int getRecentPricesLimit() { return recentPricesLimit; }
            public void setRecentPricesLimit(int recentPricesLimit) { this.recentPricesLimit = recentPricesLimit; }
        }

        public static class Rsi {
            private int minDataPoints = 15;
            private int period = 14;
            private int overboughtLevel = 70;
            private int oversoldLevel = 30;
            public int getMinDataPoints() { return minDataPoints; }
            public void setMinDataPoints(int minDataPoints) { this.minDataPoints = minDataPoints; }
            public int getPeriod() { return period; }
            public void setPeriod(int period) { this.period = period; }
            public int getOverboughtLevel() { return overboughtLevel; }
            public void setOverboughtLevel(int overboughtLevel) { this.overboughtLevel = overboughtLevel; }
            public int getOversoldLevel() { return oversoldLevel; }
            public void setOversoldLevel(int oversoldLevel) { this.oversoldLevel = oversoldLevel; }
        }

        public static class Scheduler {
            private String cron = "0 0,30 * * * *";
            private String healthCheckCron = "0 0 * * * *";
            private int cooldownMinutes = 15;
            private int maxJitterSeconds = 60;
            private boolean skipFirstRun = true;
            private int resetFailuresAfterHours = 2;

            public String getCron() { return cron; }
            public void setCron(String cron) { this.cron = cron; }
            public String getHealthCheckCron() { return healthCheckCron; }
            public void setHealthCheckCron(String healthCheckCron) { this.healthCheckCron = healthCheckCron; }
            public int getCooldownMinutes() { return cooldownMinutes; }
            public void setCooldownMinutes(int cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }
            public int getMaxJitterSeconds() { return maxJitterSeconds; }
            public void setMaxJitterSeconds(int maxJitterSeconds) { this.maxJitterSeconds = maxJitterSeconds; }
            public boolean isSkipFirstRun() { return skipFirstRun; }
            public void setSkipFirstRun(boolean skipFirstRun) { this.skipFirstRun = skipFirstRun; }
            public int getResetFailuresAfterHours() { return resetFailuresAfterHours; }
            public void setResetFailuresAfterHours(int resetFailuresAfterHours) { this.resetFailuresAfterHours = resetFailuresAfterHours; }
        }
    }

    // ===================================
    // WHATSAPP
    // ===================================
    public static class WhatsApp {
        private Api api = new Api();
        private Instance instance = new Instance();
        private Recipient recipient = new Recipient();
        private Http http = new Http();
        private RateLimit rateLimit = new RateLimit();
        private Retry retry = new Retry();

        public Api getApi() { return api; }
        public void setApi(Api api) { this.api = api; }
        public Instance getInstance() { return instance; }
        public void setInstance(Instance instance) { this.instance = instance; }
        public Recipient getRecipient() { return recipient; }
        public void setRecipient(Recipient recipient) { this.recipient = recipient; }
        public Http getHttp() { return http; }
        public void setHttp(Http http) { this.http = http; }
        public RateLimit getRateLimit() { return rateLimit; }
        public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }
        public Retry getRetry() { return retry; }
        public void setRetry(Retry retry) { this.retry = retry; }

        public static class Api {
            private String url = "https://api.ultramsg.com";
            private String token;
            public String getUrl() { return url; }
            public void setUrl(String url) { this.url = url; }
            public String getToken() { return token; }
            public void setToken(String token) { this.token = token; }
        }

        public static class Instance {
            private String id;
            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
        }

        public static class Recipient {
            private String phone;
            public String getPhone() { return phone; }
            public void setPhone(String phone) { this.phone = phone; }
        }

        public static class Http {
            private int timeoutMs = 10000;
            public int getTimeoutMs() { return timeoutMs; }
            public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
        }

        public static class RateLimit {
            private long cooldownMs = 60000;
            private int maxPerHour = 50;
            public long getCooldownMs() { return cooldownMs; }
            public void setCooldownMs(long cooldownMs) { this.cooldownMs = cooldownMs; }
            public int getMaxPerHour() { return maxPerHour; }
            public void setMaxPerHour(int maxPerHour) { this.maxPerHour = maxPerHour; }
        }

        public static class Retry {
            private int maxAttempts = 5;
            private int initialDelayMs = 1000;
            private int maxDelayMs = 30000;
            public int getMaxAttempts() { return maxAttempts; }
            public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
            public int getInitialDelayMs() { return initialDelayMs; }
            public void setInitialDelayMs(int initialDelayMs) { this.initialDelayMs = initialDelayMs; }
            public int getMaxDelayMs() { return maxDelayMs; }
            public void setMaxDelayMs(int maxDelayMs) { this.maxDelayMs = maxDelayMs; }
        }
    }

    // ===================================
    // NOTIFICATION TEMPLATES
    // ===================================
    public static class Notification {
        private Template template = new Template();
        public Template getTemplate() { return template; }
        public void setTemplate(Template template) { this.template = template; }

        public static class Template {
            private String periodic = "** Actualizacion BTC **\\n\\nPrecio: $%,.0f\\nRSI: %.2f";
            private String priceChange = "** Movimiento BTC **\\n\\nCambio: %.2f%%\\nPrecio: $%,.0f";
            private String rsiDown70 = "** RSI bajo 70 **\\n\\nPrecio: $%,.0f\\nRSI: %.2f";
            private String rsiUp30 = "** RSI sobre 30 **\\n\\nPrecio: $%,.0f\\nRSI: %.2f";
            private String classicSignal = "** SENAL BTC **\\n\\n%s\\n\\nPrecio: $%,.0f\\nRSI: %.2f";

            public String getPeriodic() { return periodic; }
            public void setPeriodic(String periodic) { this.periodic = periodic; }
            public String getPriceChange() { return priceChange; }
            public void setPriceChange(String priceChange) { this.priceChange = priceChange; }
            public String getRsiDown70() { return rsiDown70; }
            public void setRsiDown70(String rsiDown70) { this.rsiDown70 = rsiDown70; }
            public String getRsiUp30() { return rsiUp30; }
            public void setRsiUp30(String rsiUp30) { this.rsiUp30 = rsiUp30; }
            public String getClassicSignal() { return classicSignal; }
            public void setClassicSignal(String classicSignal) { this.classicSignal = classicSignal; }
        }
    }
}