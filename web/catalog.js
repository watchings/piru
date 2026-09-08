const catalogURL = "catalog/substances.json";
let substances = [];

export async function loadCatalog() {
  if (substances.length) return substances;
  const response = await fetch(catalogURL, { cache: "no-cache" });
  if (!response.ok) throw new Error(`Catalog unavailable (${response.status})`);
  const data = await response.json();
  if (!Array.isArray(data)) throw new Error("Catalog has an invalid shape");
  substances = data;
  return substances;
}

export function searchCatalog(query, limit = 50) {
  const needle = query.trim().toLocaleLowerCase();
  if (!needle) return substances.slice(0, limit);
  return substances
    .map(item => {
      const name = String(item.name ?? "").toLocaleLowerCase();
      const aliases = (item.aliases ?? []).map(alias => String(alias).toLocaleLowerCase());
      const exact = name === needle || aliases.includes(needle);
      const prefix = name.startsWith(needle) || aliases.some(alias => alias.startsWith(needle));
      const contains = name.includes(needle) || aliases.some(alias => alias.includes(needle));
      return { item, rank: exact ? 0 : prefix ? 1 : contains ? 2 : 3 };
    })
    .filter(result => result.rank < 3)
    .sort((a, b) => a.rank - b.rank || a.item.name.localeCompare(b.item.name))
    .slice(0, limit)
    .map(result => result.item);
}
