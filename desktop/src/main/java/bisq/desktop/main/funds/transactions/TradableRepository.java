/*
 * This file is part of Haveno.
 *
 * Haveno is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Haveno is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Haveno. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.funds.transactions;

import bisq.core.offer.OpenOfferManager;
import bisq.core.trade.ClosedTradableManager;
import bisq.core.trade.Tradable;
import bisq.core.trade.TradeManager;
import bisq.core.trade.failed.FailedTradesManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

@Singleton
public class TradableRepository {
    private final OpenOfferManager openOfferManager;
    private final TradeManager tradeManager;
    private final ClosedTradableManager closedTradableManager;
    private final FailedTradesManager failedTradesManager;

    @Inject
    TradableRepository(OpenOfferManager openOfferManager,
                       TradeManager tradeManager,
                       ClosedTradableManager closedTradableManager,
                       FailedTradesManager failedTradesManager) {
        this.openOfferManager = openOfferManager;
        this.tradeManager = tradeManager;
        this.closedTradableManager = closedTradableManager;
        this.failedTradesManager = failedTradesManager;
    }

    Set<Tradable> getAll() {
        return ImmutableSet.<Tradable>builder()
                .addAll(openOfferManager.getObservableList())
                .addAll(tradeManager.getObservableList())
                .addAll(closedTradableManager.getObservableList())
                .addAll(failedTradesManager.getObservableList())
                .build();
    }
}
