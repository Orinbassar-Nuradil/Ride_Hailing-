package com.ridehail.pricing;

import com.ridehail.shared.RideException;

/** Independent public API of subsystem B. */
public final class FareService {
  private final FareRepository repository;
  private final PricingEngine pricing = new PricingEngine();
  private final ReceiptGenerator generator = new ReceiptGenerator();

  public FareService(FareRepository repository) {
    this.repository = repository;
  }

  public FareReceipt issue(String id, int km, int maxFare) {
    int amount = pricing.quote(km, maxFare);
    var existing = repository.find(id);
    if (existing != null) {
      if (existing.distanceKm() != km || existing.amount() != amount)
        throw new RideException.BookingConflict("Receipt ID already has different fare data");
      existing.issue();
      return existing;
    }
    var item = generator.create(id, km, amount);
    item.issue();
    repository.save(item);
    return item;
  }

  public void voidReceipt(String id) {
    var item = repository.find(id);
    if (item != null) {
      item.voidReceipt();
      repository.save(item);
    }
  }

  public FareReceipt inspect(String id) {
    return repository.find(id);
  }
}
