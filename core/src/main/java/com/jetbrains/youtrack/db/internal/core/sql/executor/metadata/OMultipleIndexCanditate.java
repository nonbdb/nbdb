package com.jetbrains.youtrack.db.internal.core.sql.executor.metadata;

import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.index.OIndex;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.YTProperty;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.OIndexFinder.Operation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OMultipleIndexCanditate implements OIndexCandidate {

  public final List<OIndexCandidate> canditates = new ArrayList<OIndexCandidate>();

  public OMultipleIndexCanditate() {
  }

  private OMultipleIndexCanditate(Collection<OIndexCandidate> canditates) {
    this.canditates.addAll(canditates);
  }

  public void addCanditate(OIndexCandidate canditate) {
    this.canditates.add(canditate);
  }

  public List<OIndexCandidate> getCanditates() {
    return canditates;
  }

  @Override
  public String getName() {
    String name = "";
    for (OIndexCandidate oIndexCandidate : canditates) {
      name = oIndexCandidate.getName() + "|";
    }
    return name;
  }

  @Override
  public Optional<OIndexCandidate> invert() {
    // TODO: when handling operator invert it
    return Optional.of(this);
  }

  @Override
  public Operation getOperation() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<OIndexCandidate> normalize(CommandContext ctx) {
    Collection<OIndexCandidate> newCanditates = normalizeBetween(this.canditates, ctx);
    newCanditates = normalizeComposite(newCanditates, ctx);
    if (newCanditates.isEmpty()) {
      return Optional.empty();
    } else if (newCanditates.size() == 1) {
      return Optional.of(newCanditates.iterator().next());
    } else {
      return Optional.of(new OMultipleIndexCanditate(newCanditates));
    }
  }

  private Collection<OIndexCandidate> normalizeBetween(
      List<OIndexCandidate> canditates, CommandContext ctx) {
    List<OIndexCandidate> newCanditates = new ArrayList<>();
    for (int i = 0; i < canditates.size(); i++) {
      boolean matched = false;
      OIndexCandidate canditate = canditates.get(i);
      List<YTProperty> properties = canditate.properties();
      for (int z = canditates.size() - 1; z > i; z--) {
        OIndexCandidate lastCandidate = canditates.get(z);
        List<YTProperty> lastProperties = lastCandidate.properties();
        if (properties.size() == 1
            && lastProperties.size() == 1
            && properties.get(0).getName() == lastProperties.get(0).getName()) {
          if (canditate.getOperation().isRange() || lastCandidate.getOperation().isRange()) {
            newCanditates.add(new ORangeIndexCanditate(canditate.getName(), properties.get(0)));
            canditates.remove(z);
            if (z != canditates.size()) {
              z++; // Increase so it does not decrease next iteration
            }
            matched = true;
          }
        }
      }
      if (!matched) {
        newCanditates.add(canditate);
      }
    }
    return newCanditates;
  }

  private Collection<OIndexCandidate> normalizeComposite(
      Collection<OIndexCandidate> canditates, CommandContext ctx) {
    List<YTProperty> propeties = properties();
    Map<String, OIndexCandidate> newCanditates = new HashMap<>();
    for (OIndexCandidate cand : canditates) {
      if (!newCanditates.containsKey(cand.getName())) {
        OIndex index = ctx.getDatabase().getMetadata().getIndexManager().getIndex(cand.getName());
        List<YTProperty> foundProps = new ArrayList<>();
        for (String field : index.getDefinition().getFields()) {
          boolean found = false;
          for (YTProperty property : propeties) {
            if (property.getName().equals(field)) {
              found = true;
              foundProps.add(property);
              break;
            }
          }
          if (!found) {
            break;
          }
        }
        if (foundProps.size() == 1) {
          newCanditates.put(index.getName(), cand);
        } else if (!foundProps.isEmpty()) {
          newCanditates.put(
              index.getName(),
              new OIndexCandidateComposite(index.getName(), cand.getOperation(), foundProps));
        }
      }
    }
    return newCanditates.values();
  }

  @Override
  public List<YTProperty> properties() {
    List<YTProperty> props = new ArrayList<>();
    for (OIndexCandidate cand : this.canditates) {
      props.addAll(cand.properties());
    }
    return props;
  }
}