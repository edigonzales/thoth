function ls(n) {
  return {
    severity: "error",
    message: n,
    source: "check"
  };
}
function Sc(n, e, t) {
  switch (n.type) {
    case "ili2c-compiles":
      return t.ok ? [] : [
        ls(
          "Das Modell kompiliert noch nicht. Prüfe die Compiler-Meldungen und versuche es erneut."
        )
      ];
    case "contains-text":
      return e.includes(n.text) ? [] : [
        ls(
          n.message ?? `Im Modell fehlt noch der erwartete Text: "${n.text}".`
        )
      ];
    case "not-contains-text":
      return e.includes(n.text) ? [
        ls(
          n.message ?? `Der Text "${n.text}" sollte nicht mehr im Modell vorkommen.`
        )
      ] : [];
    case "custom":
      return [
        {
          severity: "warning",
          message: `Der Custom-Check "${n.id}" ist im MVP noch nicht implementiert.`,
          source: "check"
        }
      ];
  }
}
function Ac(n, e, t) {
  return n ? "Alle Prüfungen erfolgreich bestanden." : e.ok ? t.find((s) => s.source === "check")?.message ?? "Mindestens eine Aufgabenanforderung ist noch nicht erfüllt." : "Modell kompiliert noch nicht. Nutze die Compiler-Ausgabe als nächsten Schritt.";
}
function Cc(n) {
  const e = (n.lesson.checks ?? []).flatMap(
    (r) => Sc(r, n.code, n.runResult)
  ), t = [...n.runResult.diagnostics, ...e], i = n.runResult.ok && !e.some((r) => r.severity === "error"), s = {
    ...n.runResult,
    ok: i,
    diagnostics: t
  };
  return {
    ok: i,
    diagnostics: t,
    summary: Ac(i, n.runResult, t),
    runResult: s
  };
}
const ho = {
  info: 0,
  warning: 1,
  error: 2
};
function t0(n) {
  return n.reduce((e, t) => e ? ho[t.severity] > ho[e] ? t.severity : e : t.severity, void 0);
}
function i0(n) {
  return n.some((e) => e.severity === "error");
}
function Ls(n) {
  const e = [n.file, n.line, n.column].filter((t) => t !== void 0).join(":");
  return e ? `${e} ` : "";
}
function n0(n) {
  return `${Ls(n)}${n.message}`.trim();
}
function Mc(n, e) {
  const t = n.split(`
`), i = e.split(`
`), s = Math.max(t.length, i.length), r = [];
  for (let o = 0; o < s; o += 1) {
    const l = t[o], a = i[o];
    let h = "same";
    l === void 0 ? h = "added" : a === void 0 ? h = "removed" : l !== a && (h = "changed"), r.push({
      lineNumber: o + 1,
      current: l,
      solution: a,
      type: h
    });
  }
  return r;
}
const rt = {
  ready: "interlis-lab-ready",
  runStart: "interlis-lab-run-start",
  runComplete: "interlis-lab-run-complete",
  hintShown: "interlis-lab-hint-shown",
  solutionShown: "interlis-lab-solution-shown",
  reset: "interlis-lab-reset",
  error: "interlis-lab-error"
};
function ot(n, e) {
  return new CustomEvent(n, {
    detail: e,
    bubbles: !0,
    composed: !0
  });
}
const fe = {
  run: "Prüfen",
  showHint: "Hinweis anzeigen",
  showNextHint: "Nächsten Hinweis anzeigen",
  showSolution: "Lösung anzeigen",
  reset: "Zurücksetzen",
  compare: "Vergleichen",
  statusIdle: "Noch nicht geprüft",
  statusLoading: "Lädt...",
  statusReady: "Bereit",
  statusRunning: "Prüft...",
  statusSuccess: "Modell kompiliert erfolgreich",
  statusFailed: "Modell kompiliert noch nicht",
  statusError: "Fehler",
  rawOutput: "Compiler-Ausgabe"
};
function kr(n) {
  return typeof n == "object" && n !== null && !Array.isArray(n);
}
function li(n, e) {
  if (!kr(n))
    throw new Error(`${e} muss ein Objekt sein.`);
  return n;
}
function Z(n, e) {
  if (typeof n != "string" || n.trim().length === 0)
    throw new Error(`${e} muss ein nicht-leerer String sein.`);
  return n;
}
function Lt(n, e) {
  if (n != null)
    return Z(n, e);
}
function Tc(n, e) {
  if (n != null) {
    if (!Array.isArray(n) || n.some((t) => typeof t != "string"))
      throw new Error(`${e} muss ein String-Array sein.`);
    return [...n];
  }
}
function Oc(n, e) {
  const t = li(n, `Hint ${e + 1}`);
  return {
    id: Lt(t.id, `Hint ${e + 1}.id`) ?? `hint-${e + 1}`,
    title: Lt(t.title, `Hint ${e + 1}.title`),
    text: Z(t.text, `Hint ${e + 1}.text`)
  };
}
function Dc(n, e) {
  const t = li(n, `Check ${e + 1}`), i = Z(t.type, `Check ${e + 1}.type`);
  switch (i) {
    case "ili2c-compiles":
      return { type: i };
    case "contains-text":
      return {
        type: i,
        text: Z(t.text, `Check ${e + 1}.text`),
        message: Lt(t.message, `Check ${e + 1}.message`)
      };
    case "not-contains-text":
      return {
        type: i,
        text: Z(t.text, `Check ${e + 1}.text`),
        message: Lt(t.message, `Check ${e + 1}.message`)
      };
    case "custom":
      return {
        type: i,
        id: Z(t.id, `Check ${e + 1}.id`),
        params: kr(t.params) ? { ...t.params } : void 0
      };
    default:
      throw new Error(`Unbekannter Check-Typ: ${i}`);
  }
}
function Ec(n) {
  const e = li(n, "reflection");
  return {
    question: Z(e.question, "reflection.question"),
    kind: e.kind === void 0 ? void 0 : Z(e.kind, "reflection.kind"),
    options: Tc(e.options, "reflection.options"),
    answer: Lt(e.answer, "reflection.answer")
  };
}
function Lc(n) {
  if (n != null) {
    if (!Array.isArray(n))
      throw new Error("files muss ein Array sein.");
    return n.map((e, t) => {
      const i = li(e, `File ${t + 1}`);
      return {
        path: Z(i.path, `File ${t + 1}.path`),
        language: i.language === void 0 ? void 0 : Z(i.language, `File ${t + 1}.language`),
        content: Z(i.content, `File ${t + 1}.content`),
        readonly: typeof i.readonly == "boolean" ? i.readonly : void 0
      };
    });
  }
}
function Rc(n) {
  if (n != null) {
    if (!Array.isArray(n))
      throw new Error("actions muss ein Array sein.");
    return n.map((e, t) => {
      const i = li(e, `Action ${t + 1}`);
      return {
        id: Z(i.id, `Action ${t + 1}.id`),
        label: Z(i.label, `Action ${t + 1}.label`),
        kind: Z(i.kind, `Action ${t + 1}.kind`),
        primary: typeof i.primary == "boolean" ? i.primary : void 0
      };
    });
  }
}
function zl(n) {
  const e = li(n, "Lesson"), t = Z(e.type, "type");
  if (!["guided", "debugging", "challenge", "worked-example"].includes(t))
    throw new Error(`Ungültiger Lesson-Typ: ${t}`);
  const i = e.level === void 0 ? void 0 : Z(e.level, "level");
  if (i && !["beginner", "intermediate", "advanced"].includes(i))
    throw new Error(`Ungültiges Level: ${i}`);
  return {
    id: Z(e.id, "id"),
    title: Z(e.title, "title"),
    type: t,
    level: i,
    goal: Z(e.goal, "goal"),
    explanation: Lt(e.explanation, "explanation"),
    task: Z(e.task, "task"),
    initialCode: Z(e.initialCode, "initialCode"),
    solution: Lt(e.solution, "solution"),
    hints: Array.isArray(e.hints) ? e.hints.map(Oc) : void 0,
    checks: Array.isArray(e.checks) ? e.checks.map(Dc) : void 0,
    reflection: e.reflection ? Ec(e.reflection) : void 0,
    files: Lc(e.files),
    actions: Rc(e.actions),
    metadata: kr(e.metadata) ? { ...e.metadata } : void 0
  };
}
async function Bc(n, e = fetch) {
  const t = await e(n);
  if (!t.ok)
    throw new Error(`Die Aufgabe konnte nicht geladen werden (${t.status} ${t.statusText}).`);
  const i = await t.json();
  return zl(i);
}
function co(n, e = {}) {
  const t = e.code ?? n.initialCode;
  return {
    status: "ready",
    code: t,
    visibleHintCount: 0,
    solutionVisible: e.showSolution ?? !1,
    dirty: t !== n.initialCode,
    feedbackMessage: void 0,
    error: void 0
  };
}
function Pc(n) {
  return {
    status: "loading",
    code: n?.code ?? "",
    visibleHintCount: n?.visibleHintCount ?? 0,
    solutionVisible: n?.solutionVisible ?? !1,
    lastResult: n?.lastResult,
    error: void 0,
    dirty: n?.dirty ?? !1,
    feedbackMessage: void 0
  };
}
function s0(n, e, t) {
  return {
    ...n,
    code: e,
    dirty: e !== t.initialCode
  };
}
function Ic(n) {
  return {
    ...n,
    status: "running",
    error: void 0,
    feedbackMessage: void 0
  };
}
function $c(n, e, t) {
  return {
    ...n,
    status: e.ok ? "success" : "failed",
    lastResult: e,
    error: void 0,
    feedbackMessage: t
  };
}
function Nc(n, e) {
  return {
    ...n,
    visibleHintCount: Math.min(e, n.visibleHintCount + 1)
  };
}
function Hc(n) {
  return {
    ...n,
    solutionVisible: !0
  };
}
function Vc(n, e) {
  return {
    ...n,
    status: "error",
    error: e,
    feedbackMessage: void 0
  };
}
class Fc {
  constructor(e) {
    this.textarea = e;
  }
  getValue() {
    return this.textarea.value;
  }
  setValue(e) {
    this.textarea.value !== e && (this.textarea.value = e);
  }
  setDiagnostics(e) {
    const t = e.find((i) => i.severity === "error");
    this.textarea.setAttribute("aria-invalid", t ? "true" : "false"), this.textarea.title = t?.message ?? "";
  }
  setReadOnly(e) {
    this.textarea.readOnly = e;
  }
  focus() {
    this.textarea.focus();
  }
  dispose() {
    this.textarea.removeAttribute("aria-invalid"), this.textarea.title = "";
  }
}
let Rs = [], Ul = [];
(() => {
  let n = "lc,34,7n,7,7b,19,,,,2,,2,,,20,b,1c,l,g,,2t,7,2,6,2,2,,4,z,,u,r,2j,b,1m,9,9,,o,4,,9,,3,,5,17,3,3b,f,,w,1j,,,,4,8,4,,3,7,a,2,t,,1m,,,,2,4,8,,9,,a,2,q,,2,2,1l,,4,2,4,2,2,3,3,,u,2,3,,b,2,1l,,4,5,,2,4,,k,2,m,6,,,1m,,,2,,4,8,,7,3,a,2,u,,1n,,,,c,,9,,14,,3,,1l,3,5,3,,4,7,2,b,2,t,,1m,,2,,2,,3,,5,2,7,2,b,2,s,2,1l,2,,,2,4,8,,9,,a,2,t,,20,,4,,2,3,,,8,,29,,2,7,c,8,2q,,2,9,b,6,22,2,r,,,,,,1j,e,,5,,2,5,b,,10,9,,2u,4,,6,,2,2,2,p,2,4,3,g,4,d,,2,2,6,,f,,jj,3,qa,3,t,3,t,2,u,2,1s,2,,7,8,,2,b,9,,19,3,3b,2,y,,3a,3,4,2,9,,6,3,63,2,2,,1m,,,7,,,,,2,8,6,a,2,,1c,h,1r,4,1c,7,,,5,,14,9,c,2,w,4,2,2,,3,1k,,,2,3,,,3,1m,8,2,2,48,3,,d,,7,4,,6,,3,2,5i,1m,,5,ek,,5f,x,2da,3,3x,,2o,w,fe,6,2x,2,n9w,4,,a,w,2,28,2,7k,,3,,4,,p,2,5,,47,2,q,i,d,,12,8,p,b,1a,3,1c,,2,4,2,2,13,,1v,6,2,2,2,2,c,,8,,1b,,1f,,,3,2,2,5,2,,,16,2,8,,6m,,2,,4,,fn4,,kh,g,g,g,a6,2,gt,,6a,,45,5,1ae,3,,2,5,4,14,3,4,,4l,2,fx,4,ar,2,49,b,4w,,1i,f,1k,3,1d,4,2,2,1x,3,10,5,,8,1q,,c,2,1g,9,a,4,2,,2n,3,2,,,2,6,,4g,,3,8,l,2,1l,2,,,,,m,,e,7,3,5,5f,8,2,3,,,n,,29,,2,6,,,2,,,2,,2,6j,,2,4,6,2,,2,r,2,2d,8,2,,,2,2y,,,,2,6,,,2t,3,2,4,,5,77,9,,2,6t,,a,2,,,4,,40,4,2,2,4,,w,a,14,6,2,4,8,,9,6,2,3,1a,d,,2,ba,7,,6,,,2a,m,2,7,,2,,2,3e,6,3,,,2,,7,,,20,2,3,,,,9n,2,f0b,5,1n,7,t4,,1r,4,29,,f5k,2,43q,,,3,4,5,8,8,2,7,u,4,44,3,1iz,1j,4,1e,8,,e,,m,5,,f,11s,7,,h,2,7,,2,,5,79,7,c5,4,15s,7,31,7,240,5,gx7k,2o,3k,6o".split(",").map((e) => e ? parseInt(e, 36) : 1);
  for (let e = 0, t = 0; e < n.length; e++)
    (e % 2 ? Ul : Rs).push(t = t + n[e]);
})();
function Wc(n) {
  if (n < 768) return !1;
  for (let e = 0, t = Rs.length; ; ) {
    let i = e + t >> 1;
    if (n < Rs[i]) t = i;
    else if (n >= Ul[i]) e = i + 1;
    else return !0;
    if (e == t) return !1;
  }
}
function fo(n) {
  return n >= 127462 && n <= 127487;
}
const uo = 8205;
function _c(n, e, t = !0, i = !0) {
  return (t ? jl : zc)(n, e, i);
}
function jl(n, e, t) {
  if (e == n.length) return e;
  e && ql(n.charCodeAt(e)) && Kl(n.charCodeAt(e - 1)) && e--;
  let i = as(n, e);
  for (e += po(i); e < n.length; ) {
    let s = as(n, e);
    if (i == uo || s == uo || t && Wc(s))
      e += po(s), i = s;
    else if (fo(s)) {
      let r = 0, o = e - 2;
      for (; o >= 0 && fo(as(n, o)); )
        r++, o -= 2;
      if (r % 2 == 0) break;
      e += 2;
    } else
      break;
  }
  return e;
}
function zc(n, e, t) {
  for (; e > 0; ) {
    let i = jl(n, e - 2, t);
    if (i < e) return i;
    e--;
  }
  return 0;
}
function as(n, e) {
  let t = n.charCodeAt(e);
  if (!Kl(t) || e + 1 == n.length) return t;
  let i = n.charCodeAt(e + 1);
  return ql(i) ? (t - 55296 << 10) + (i - 56320) + 65536 : t;
}
function ql(n) {
  return n >= 56320 && n < 57344;
}
function Kl(n) {
  return n >= 55296 && n < 56320;
}
function po(n) {
  return n < 65536 ? 1 : 2;
}
class $ {
  /**
  Get the line description around the given position.
  */
  lineAt(e) {
    if (e < 0 || e > this.length)
      throw new RangeError(`Invalid position ${e} in document of length ${this.length}`);
    return this.lineInner(e, !1, 1, 0);
  }
  /**
  Get the description for the given (1-based) line number.
  */
  line(e) {
    if (e < 1 || e > this.lines)
      throw new RangeError(`Invalid line number ${e} in ${this.lines}-line document`);
    return this.lineInner(e, !0, 1, 0);
  }
  /**
  Replace a range of the text with the given content.
  */
  replace(e, t, i) {
    [e, t] = Jt(this, e, t);
    let s = [];
    return this.decompose(
      0,
      e,
      s,
      2
      /* Open.To */
    ), i.length && i.decompose(
      0,
      i.length,
      s,
      3
      /* Open.To */
    ), this.decompose(
      t,
      this.length,
      s,
      1
      /* Open.From */
    ), qe.from(s, this.length - (t - e) + i.length);
  }
  /**
  Append another document to this one.
  */
  append(e) {
    return this.replace(this.length, this.length, e);
  }
  /**
  Retrieve the text between the given points.
  */
  slice(e, t = this.length) {
    [e, t] = Jt(this, e, t);
    let i = [];
    return this.decompose(e, t, i, 0), qe.from(i, t - e);
  }
  /**
  Test whether this text is equal to another instance.
  */
  eq(e) {
    if (e == this)
      return !0;
    if (e.length != this.length || e.lines != this.lines)
      return !1;
    let t = this.scanIdentical(e, 1), i = this.length - this.scanIdentical(e, -1), s = new wi(this), r = new wi(e);
    for (let o = t, l = t; ; ) {
      if (s.next(o), r.next(o), o = 0, s.lineBreak != r.lineBreak || s.done != r.done || s.value != r.value)
        return !1;
      if (l += s.value.length, s.done || l >= i)
        return !0;
    }
  }
  /**
  Iterate over the text. When `dir` is `-1`, iteration happens
  from end to start. This will return lines and the breaks between
  them as separate strings.
  */
  iter(e = 1) {
    return new wi(this, e);
  }
  /**
  Iterate over a range of the text. When `from` > `to`, the
  iterator will run in reverse.
  */
  iterRange(e, t = this.length) {
    return new Gl(this, e, t);
  }
  /**
  Return a cursor that iterates over the given range of lines,
  _without_ returning the line breaks between, and yielding empty
  strings for empty lines.
  
  When `from` and `to` are given, they should be 1-based line numbers.
  */
  iterLines(e, t) {
    let i;
    if (e == null)
      i = this.iter();
    else {
      t == null && (t = this.lines + 1);
      let s = this.line(e).from;
      i = this.iterRange(s, Math.max(s, t == this.lines + 1 ? this.length : t <= 1 ? 0 : this.line(t - 1).to));
    }
    return new Jl(i);
  }
  /**
  Return the document as a string, using newline characters to
  separate lines.
  */
  toString() {
    return this.sliceString(0);
  }
  /**
  Convert the document to an array of lines (which can be
  deserialized again via [`Text.of`](https://codemirror.net/6/docs/ref/#state.Text^of)).
  */
  toJSON() {
    let e = [];
    return this.flatten(e), e;
  }
  /**
  @internal
  */
  constructor() {
  }
  /**
  Create a `Text` instance for the given array of lines.
  */
  static of(e) {
    if (e.length == 0)
      throw new RangeError("A document must have at least one line");
    return e.length == 1 && !e[0] ? $.empty : e.length <= 32 ? new Y(e) : qe.from(Y.split(e, []));
  }
}
class Y extends $ {
  constructor(e, t = Uc(e)) {
    super(), this.text = e, this.length = t;
  }
  get lines() {
    return this.text.length;
  }
  get children() {
    return null;
  }
  lineInner(e, t, i, s) {
    for (let r = 0; ; r++) {
      let o = this.text[r], l = s + o.length;
      if ((t ? i : l) >= e)
        return new jc(s, l, i, o);
      s = l + 1, i++;
    }
  }
  decompose(e, t, i, s) {
    let r = e <= 0 && t >= this.length ? this : new Y(go(this.text, e, t), Math.min(t, this.length) - Math.max(0, e));
    if (s & 1) {
      let o = i.pop(), l = bn(r.text, o.text.slice(), 0, r.length);
      if (l.length <= 32)
        i.push(new Y(l, o.length + r.length));
      else {
        let a = l.length >> 1;
        i.push(new Y(l.slice(0, a)), new Y(l.slice(a)));
      }
    } else
      i.push(r);
  }
  replace(e, t, i) {
    if (!(i instanceof Y))
      return super.replace(e, t, i);
    [e, t] = Jt(this, e, t);
    let s = bn(this.text, bn(i.text, go(this.text, 0, e)), t), r = this.length + i.length - (t - e);
    return s.length <= 32 ? new Y(s, r) : qe.from(Y.split(s, []), r);
  }
  sliceString(e, t = this.length, i = `
`) {
    [e, t] = Jt(this, e, t);
    let s = "";
    for (let r = 0, o = 0; r <= t && o < this.text.length; o++) {
      let l = this.text[o], a = r + l.length;
      r > e && o && (s += i), e < a && t > r && (s += l.slice(Math.max(0, e - r), t - r)), r = a + 1;
    }
    return s;
  }
  flatten(e) {
    for (let t of this.text)
      e.push(t);
  }
  scanIdentical() {
    return 0;
  }
  static split(e, t) {
    let i = [], s = -1;
    for (let r of e)
      i.push(r), s += r.length + 1, i.length == 32 && (t.push(new Y(i, s)), i = [], s = -1);
    return s > -1 && t.push(new Y(i, s)), t;
  }
}
class qe extends $ {
  constructor(e, t) {
    super(), this.children = e, this.length = t, this.lines = 0;
    for (let i of e)
      this.lines += i.lines;
  }
  lineInner(e, t, i, s) {
    for (let r = 0; ; r++) {
      let o = this.children[r], l = s + o.length, a = i + o.lines - 1;
      if ((t ? a : l) >= e)
        return o.lineInner(e, t, i, s);
      s = l + 1, i = a + 1;
    }
  }
  decompose(e, t, i, s) {
    for (let r = 0, o = 0; o <= t && r < this.children.length; r++) {
      let l = this.children[r], a = o + l.length;
      if (e <= a && t >= o) {
        let h = s & ((o <= e ? 1 : 0) | (a >= t ? 2 : 0));
        o >= e && a <= t && !h ? i.push(l) : l.decompose(e - o, t - o, i, h);
      }
      o = a + 1;
    }
  }
  replace(e, t, i) {
    if ([e, t] = Jt(this, e, t), i.lines < this.lines)
      for (let s = 0, r = 0; s < this.children.length; s++) {
        let o = this.children[s], l = r + o.length;
        if (e >= r && t <= l) {
          let a = o.replace(e - r, t - r, i), h = this.lines - o.lines + a.lines;
          if (a.lines < h >> 4 && a.lines > h >> 6) {
            let c = this.children.slice();
            return c[s] = a, new qe(c, this.length - (t - e) + i.length);
          }
          return super.replace(r, l, a);
        }
        r = l + 1;
      }
    return super.replace(e, t, i);
  }
  sliceString(e, t = this.length, i = `
`) {
    [e, t] = Jt(this, e, t);
    let s = "";
    for (let r = 0, o = 0; r < this.children.length && o <= t; r++) {
      let l = this.children[r], a = o + l.length;
      o > e && r && (s += i), e < a && t > o && (s += l.sliceString(e - o, t - o, i)), o = a + 1;
    }
    return s;
  }
  flatten(e) {
    for (let t of this.children)
      t.flatten(e);
  }
  scanIdentical(e, t) {
    if (!(e instanceof qe))
      return 0;
    let i = 0, [s, r, o, l] = t > 0 ? [0, 0, this.children.length, e.children.length] : [this.children.length - 1, e.children.length - 1, -1, -1];
    for (; ; s += t, r += t) {
      if (s == o || r == l)
        return i;
      let a = this.children[s], h = e.children[r];
      if (a != h)
        return i + a.scanIdentical(h, t);
      i += a.length + 1;
    }
  }
  static from(e, t = e.reduce((i, s) => i + s.length + 1, -1)) {
    let i = 0;
    for (let d of e)
      i += d.lines;
    if (i < 32) {
      let d = [];
      for (let p of e)
        p.flatten(d);
      return new Y(d, t);
    }
    let s = Math.max(
      32,
      i >> 5
      /* Tree.BranchShift */
    ), r = s << 1, o = s >> 1, l = [], a = 0, h = -1, c = [];
    function f(d) {
      let p;
      if (d.lines > r && d instanceof qe)
        for (let g of d.children)
          f(g);
      else d.lines > o && (a > o || !a) ? (u(), l.push(d)) : d instanceof Y && a && (p = c[c.length - 1]) instanceof Y && d.lines + p.lines <= 32 ? (a += d.lines, h += d.length + 1, c[c.length - 1] = new Y(p.text.concat(d.text), p.length + 1 + d.length)) : (a + d.lines > s && u(), a += d.lines, h += d.length + 1, c.push(d));
    }
    function u() {
      a != 0 && (l.push(c.length == 1 ? c[0] : qe.from(c, h)), h = -1, a = c.length = 0);
    }
    for (let d of e)
      f(d);
    return u(), l.length == 1 ? l[0] : new qe(l, t);
  }
}
$.empty = /* @__PURE__ */ new Y([""], 0);
function Uc(n) {
  let e = -1;
  for (let t of n)
    e += t.length + 1;
  return e;
}
function bn(n, e, t = 0, i = 1e9) {
  for (let s = 0, r = 0, o = !0; r < n.length && s <= i; r++) {
    let l = n[r], a = s + l.length;
    a >= t && (a > i && (l = l.slice(0, i - s)), s < t && (l = l.slice(t - s)), o ? (e[e.length - 1] += l, o = !1) : e.push(l)), s = a + 1;
  }
  return e;
}
function go(n, e, t) {
  return bn(n, [""], e, t);
}
class wi {
  constructor(e, t = 1) {
    this.dir = t, this.done = !1, this.lineBreak = !1, this.value = "", this.nodes = [e], this.offsets = [t > 0 ? 1 : (e instanceof Y ? e.text.length : e.children.length) << 1];
  }
  nextInner(e, t) {
    for (this.done = this.lineBreak = !1; ; ) {
      let i = this.nodes.length - 1, s = this.nodes[i], r = this.offsets[i], o = r >> 1, l = s instanceof Y ? s.text.length : s.children.length;
      if (o == (t > 0 ? l : 0)) {
        if (i == 0)
          return this.done = !0, this.value = "", this;
        t > 0 && this.offsets[i - 1]++, this.nodes.pop(), this.offsets.pop();
      } else if ((r & 1) == (t > 0 ? 0 : 1)) {
        if (this.offsets[i] += t, e == 0)
          return this.lineBreak = !0, this.value = `
`, this;
        e--;
      } else if (s instanceof Y) {
        let a = s.text[o + (t < 0 ? -1 : 0)];
        if (this.offsets[i] += t, a.length > Math.max(0, e))
          return this.value = e == 0 ? a : t > 0 ? a.slice(e) : a.slice(0, a.length - e), this;
        e -= a.length;
      } else {
        let a = s.children[o + (t < 0 ? -1 : 0)];
        e > a.length ? (e -= a.length, this.offsets[i] += t) : (t < 0 && this.offsets[i]--, this.nodes.push(a), this.offsets.push(t > 0 ? 1 : (a instanceof Y ? a.text.length : a.children.length) << 1));
      }
    }
  }
  next(e = 0) {
    return e < 0 && (this.nextInner(-e, -this.dir), e = this.value.length), this.nextInner(e, this.dir);
  }
}
class Gl {
  constructor(e, t, i) {
    this.value = "", this.done = !1, this.cursor = new wi(e, t > i ? -1 : 1), this.pos = t > i ? e.length : 0, this.from = Math.min(t, i), this.to = Math.max(t, i);
  }
  nextInner(e, t) {
    if (t < 0 ? this.pos <= this.from : this.pos >= this.to)
      return this.value = "", this.done = !0, this;
    e += Math.max(0, t < 0 ? this.pos - this.to : this.from - this.pos);
    let i = t < 0 ? this.pos - this.from : this.to - this.pos;
    e > i && (e = i), i -= e;
    let { value: s } = this.cursor.next(e);
    return this.pos += (s.length + e) * t, this.value = s.length <= i ? s : t < 0 ? s.slice(s.length - i) : s.slice(0, i), this.done = !this.value, this;
  }
  next(e = 0) {
    return e < 0 ? e = Math.max(e, this.from - this.pos) : e > 0 && (e = Math.min(e, this.to - this.pos)), this.nextInner(e, this.cursor.dir);
  }
  get lineBreak() {
    return this.cursor.lineBreak && this.value != "";
  }
}
class Jl {
  constructor(e) {
    this.inner = e, this.afterBreak = !0, this.value = "", this.done = !1;
  }
  next(e = 0) {
    let { done: t, lineBreak: i, value: s } = this.inner.next(e);
    return t && this.afterBreak ? (this.value = "", this.afterBreak = !1) : t ? (this.done = !0, this.value = "") : i ? this.afterBreak ? this.value = "" : (this.afterBreak = !0, this.next()) : (this.value = s, this.afterBreak = !1), this;
  }
  get lineBreak() {
    return !1;
  }
}
typeof Symbol < "u" && ($.prototype[Symbol.iterator] = function() {
  return this.iter();
}, wi.prototype[Symbol.iterator] = Gl.prototype[Symbol.iterator] = Jl.prototype[Symbol.iterator] = function() {
  return this;
});
class jc {
  /**
  @internal
  */
  constructor(e, t, i, s) {
    this.from = e, this.to = t, this.number = i, this.text = s;
  }
  /**
  The length of the line (not including any line break after it).
  */
  get length() {
    return this.to - this.from;
  }
}
function Jt(n, e, t) {
  return e = Math.max(0, Math.min(n.length, e)), [e, Math.max(e, Math.min(n.length, t))];
}
function ae(n, e, t = !0, i = !0) {
  return _c(n, e, t, i);
}
function qc(n) {
  return n >= 56320 && n < 57344;
}
function Kc(n) {
  return n >= 55296 && n < 56320;
}
function Gc(n, e) {
  let t = n.charCodeAt(e);
  if (!Kc(t) || e + 1 == n.length)
    return t;
  let i = n.charCodeAt(e + 1);
  return qc(i) ? (t - 55296 << 10) + (i - 56320) + 65536 : t;
}
function Jc(n) {
  return n < 65536 ? 1 : 2;
}
const Bs = /\r\n?|\n/;
var pe = /* @__PURE__ */ (function(n) {
  return n[n.Simple = 0] = "Simple", n[n.TrackDel = 1] = "TrackDel", n[n.TrackBefore = 2] = "TrackBefore", n[n.TrackAfter = 3] = "TrackAfter", n;
})(pe || (pe = {}));
class Xe {
  // Sections are encoded as pairs of integers. The first is the
  // length in the current document, and the second is -1 for
  // unaffected sections, and the length of the replacement content
  // otherwise. So an insertion would be (0, n>0), a deletion (n>0,
  // 0), and a replacement two positive numbers.
  /**
  @internal
  */
  constructor(e) {
    this.sections = e;
  }
  /**
  The length of the document before the change.
  */
  get length() {
    let e = 0;
    for (let t = 0; t < this.sections.length; t += 2)
      e += this.sections[t];
    return e;
  }
  /**
  The length of the document after the change.
  */
  get newLength() {
    let e = 0;
    for (let t = 0; t < this.sections.length; t += 2) {
      let i = this.sections[t + 1];
      e += i < 0 ? this.sections[t] : i;
    }
    return e;
  }
  /**
  False when there are actual changes in this set.
  */
  get empty() {
    return this.sections.length == 0 || this.sections.length == 2 && this.sections[1] < 0;
  }
  /**
  Iterate over the unchanged parts left by these changes. `posA`
  provides the position of the range in the old document, `posB`
  the new position in the changed document.
  */
  iterGaps(e) {
    for (let t = 0, i = 0, s = 0; t < this.sections.length; ) {
      let r = this.sections[t++], o = this.sections[t++];
      o < 0 ? (e(i, s, r), s += r) : s += o, i += r;
    }
  }
  /**
  Iterate over the ranges changed by these changes. (See
  [`ChangeSet.iterChanges`](https://codemirror.net/6/docs/ref/#state.ChangeSet.iterChanges) for a
  variant that also provides you with the inserted text.)
  `fromA`/`toA` provides the extent of the change in the starting
  document, `fromB`/`toB` the extent of the replacement in the
  changed document.
  
  When `individual` is true, adjacent changes (which are kept
  separate for [position mapping](https://codemirror.net/6/docs/ref/#state.ChangeDesc.mapPos)) are
  reported separately.
  */
  iterChangedRanges(e, t = !1) {
    Ps(this, e, t);
  }
  /**
  Get a description of the inverted form of these changes.
  */
  get invertedDesc() {
    let e = [];
    for (let t = 0; t < this.sections.length; ) {
      let i = this.sections[t++], s = this.sections[t++];
      s < 0 ? e.push(i, s) : e.push(s, i);
    }
    return new Xe(e);
  }
  /**
  Compute the combined effect of applying another set of changes
  after this one. The length of the document after this set should
  match the length before `other`.
  */
  composeDesc(e) {
    return this.empty ? e : e.empty ? this : Yl(this, e);
  }
  /**
  Map this description, which should start with the same document
  as `other`, over another set of changes, so that it can be
  applied after it. When `before` is true, map as if the changes
  in `this` happened before the ones in `other`.
  */
  mapDesc(e, t = !1) {
    return e.empty ? this : Is(this, e, t);
  }
  mapPos(e, t = -1, i = pe.Simple) {
    let s = 0, r = 0;
    for (let o = 0; o < this.sections.length; ) {
      let l = this.sections[o++], a = this.sections[o++], h = s + l;
      if (a < 0) {
        if (h > e)
          return r + (e - s);
        r += l;
      } else {
        if (i != pe.Simple && h >= e && (i == pe.TrackDel && s < e && h > e || i == pe.TrackBefore && s < e || i == pe.TrackAfter && h > e))
          return null;
        if (h > e || h == e && t < 0 && !l)
          return e == s || t < 0 ? r : r + a;
        r += a;
      }
      s = h;
    }
    if (e > s)
      throw new RangeError(`Position ${e} is out of range for changeset of length ${s}`);
    return r;
  }
  /**
  Check whether these changes touch a given range. When one of the
  changes entirely covers the range, the string `"cover"` is
  returned.
  */
  touchesRange(e, t = e) {
    for (let i = 0, s = 0; i < this.sections.length && s <= t; ) {
      let r = this.sections[i++], o = this.sections[i++], l = s + r;
      if (o >= 0 && s <= t && l >= e)
        return s < e && l > t ? "cover" : !0;
      s = l;
    }
    return !1;
  }
  /**
  @internal
  */
  toString() {
    let e = "";
    for (let t = 0; t < this.sections.length; ) {
      let i = this.sections[t++], s = this.sections[t++];
      e += (e ? " " : "") + i + (s >= 0 ? ":" + s : "");
    }
    return e;
  }
  /**
  Serialize this change desc to a JSON-representable value.
  */
  toJSON() {
    return this.sections;
  }
  /**
  Create a change desc from its JSON representation (as produced
  by [`toJSON`](https://codemirror.net/6/docs/ref/#state.ChangeDesc.toJSON).
  */
  static fromJSON(e) {
    if (!Array.isArray(e) || e.length % 2 || e.some((t) => typeof t != "number"))
      throw new RangeError("Invalid JSON representation of ChangeDesc");
    return new Xe(e);
  }
  /**
  @internal
  */
  static create(e) {
    return new Xe(e);
  }
}
class ee extends Xe {
  constructor(e, t) {
    super(e), this.inserted = t;
  }
  /**
  Apply the changes to a document, returning the modified
  document.
  */
  apply(e) {
    if (this.length != e.length)
      throw new RangeError("Applying change set to a document with the wrong length");
    return Ps(this, (t, i, s, r, o) => e = e.replace(s, s + (i - t), o), !1), e;
  }
  mapDesc(e, t = !1) {
    return Is(this, e, t, !0);
  }
  /**
  Given the document as it existed _before_ the changes, return a
  change set that represents the inverse of this set, which could
  be used to go from the document created by the changes back to
  the document as it existed before the changes.
  */
  invert(e) {
    let t = this.sections.slice(), i = [];
    for (let s = 0, r = 0; s < t.length; s += 2) {
      let o = t[s], l = t[s + 1];
      if (l >= 0) {
        t[s] = l, t[s + 1] = o;
        let a = s >> 1;
        for (; i.length < a; )
          i.push($.empty);
        i.push(o ? e.slice(r, r + o) : $.empty);
      }
      r += o;
    }
    return new ee(t, i);
  }
  /**
  Combine two subsequent change sets into a single set. `other`
  must start in the document produced by `this`. If `this` goes
  `docA` → `docB` and `other` represents `docB` → `docC`, the
  returned value will represent the change `docA` → `docC`.
  */
  compose(e) {
    return this.empty ? e : e.empty ? this : Yl(this, e, !0);
  }
  /**
  Given another change set starting in the same document, maps this
  change set over the other, producing a new change set that can be
  applied to the document produced by applying `other`. When
  `before` is `true`, order changes as if `this` comes before
  `other`, otherwise (the default) treat `other` as coming first.
  
  Given two changes `A` and `B`, `A.compose(B.map(A))` and
  `B.compose(A.map(B, true))` will produce the same document. This
  provides a basic form of [operational
  transformation](https://en.wikipedia.org/wiki/Operational_transformation),
  and can be used for collaborative editing.
  */
  map(e, t = !1) {
    return e.empty ? this : Is(this, e, t, !0);
  }
  /**
  Iterate over the changed ranges in the document, calling `f` for
  each, with the range in the original document (`fromA`-`toA`)
  and the range that replaces it in the new document
  (`fromB`-`toB`).
  
  When `individual` is true, adjacent changes are reported
  separately.
  */
  iterChanges(e, t = !1) {
    Ps(this, e, t);
  }
  /**
  Get a [change description](https://codemirror.net/6/docs/ref/#state.ChangeDesc) for this change
  set.
  */
  get desc() {
    return Xe.create(this.sections);
  }
  /**
  @internal
  */
  filter(e) {
    let t = [], i = [], s = [], r = new Oi(this);
    e: for (let o = 0, l = 0; ; ) {
      let a = o == e.length ? 1e9 : e[o++];
      for (; l < a || l == a && r.len == 0; ) {
        if (r.done)
          break e;
        let c = Math.min(r.len, a - l);
        le(s, c, -1);
        let f = r.ins == -1 ? -1 : r.off == 0 ? r.ins : 0;
        le(t, c, f), f > 0 && ft(i, t, r.text), r.forward(c), l += c;
      }
      let h = e[o++];
      for (; l < h; ) {
        if (r.done)
          break e;
        let c = Math.min(r.len, h - l);
        le(t, c, -1), le(s, c, r.ins == -1 ? -1 : r.off == 0 ? r.ins : 0), r.forward(c), l += c;
      }
    }
    return {
      changes: new ee(t, i),
      filtered: Xe.create(s)
    };
  }
  /**
  Serialize this change set to a JSON-representable value.
  */
  toJSON() {
    let e = [];
    for (let t = 0; t < this.sections.length; t += 2) {
      let i = this.sections[t], s = this.sections[t + 1];
      s < 0 ? e.push(i) : s == 0 ? e.push([i]) : e.push([i].concat(this.inserted[t >> 1].toJSON()));
    }
    return e;
  }
  /**
  Create a change set for the given changes, for a document of the
  given length, using `lineSep` as line separator.
  */
  static of(e, t, i) {
    let s = [], r = [], o = 0, l = null;
    function a(c = !1) {
      if (!c && !s.length)
        return;
      o < t && le(s, t - o, -1);
      let f = new ee(s, r);
      l = l ? l.compose(f.map(l)) : f, s = [], r = [], o = 0;
    }
    function h(c) {
      if (Array.isArray(c))
        for (let f of c)
          h(f);
      else if (c instanceof ee) {
        if (c.length != t)
          throw new RangeError(`Mismatched change set length (got ${c.length}, expected ${t})`);
        a(), l = l ? l.compose(c.map(l)) : c;
      } else {
        let { from: f, to: u = f, insert: d } = c;
        if (f > u || f < 0 || u > t)
          throw new RangeError(`Invalid change range ${f} to ${u} (in doc of length ${t})`);
        let p = d ? typeof d == "string" ? $.of(d.split(i || Bs)) : d : $.empty, g = p.length;
        if (f == u && g == 0)
          return;
        f < o && a(), f > o && le(s, f - o, -1), le(s, u - f, g), ft(r, s, p), o = u;
      }
    }
    return h(e), a(!l), l;
  }
  /**
  Create an empty changeset of the given length.
  */
  static empty(e) {
    return new ee(e ? [e, -1] : [], []);
  }
  /**
  Create a changeset from its JSON representation (as produced by
  [`toJSON`](https://codemirror.net/6/docs/ref/#state.ChangeSet.toJSON).
  */
  static fromJSON(e) {
    if (!Array.isArray(e))
      throw new RangeError("Invalid JSON representation of ChangeSet");
    let t = [], i = [];
    for (let s = 0; s < e.length; s++) {
      let r = e[s];
      if (typeof r == "number")
        t.push(r, -1);
      else {
        if (!Array.isArray(r) || typeof r[0] != "number" || r.some((o, l) => l && typeof o != "string"))
          throw new RangeError("Invalid JSON representation of ChangeSet");
        if (r.length == 1)
          t.push(r[0], 0);
        else {
          for (; i.length < s; )
            i.push($.empty);
          i[s] = $.of(r.slice(1)), t.push(r[0], i[s].length);
        }
      }
    }
    return new ee(t, i);
  }
  /**
  @internal
  */
  static createSet(e, t) {
    return new ee(e, t);
  }
}
function le(n, e, t, i = !1) {
  if (e == 0 && t <= 0)
    return;
  let s = n.length - 2;
  s >= 0 && t <= 0 && t == n[s + 1] ? n[s] += e : s >= 0 && e == 0 && n[s] == 0 ? n[s + 1] += t : i ? (n[s] += e, n[s + 1] += t) : n.push(e, t);
}
function ft(n, e, t) {
  if (t.length == 0)
    return;
  let i = e.length - 2 >> 1;
  if (i < n.length)
    n[n.length - 1] = n[n.length - 1].append(t);
  else {
    for (; n.length < i; )
      n.push($.empty);
    n.push(t);
  }
}
function Ps(n, e, t) {
  let i = n.inserted;
  for (let s = 0, r = 0, o = 0; o < n.sections.length; ) {
    let l = n.sections[o++], a = n.sections[o++];
    if (a < 0)
      s += l, r += l;
    else {
      let h = s, c = r, f = $.empty;
      for (; h += l, c += a, a && i && (f = f.append(i[o - 2 >> 1])), !(t || o == n.sections.length || n.sections[o + 1] < 0); )
        l = n.sections[o++], a = n.sections[o++];
      e(s, h, r, c, f), s = h, r = c;
    }
  }
}
function Is(n, e, t, i = !1) {
  let s = [], r = i ? [] : null, o = new Oi(n), l = new Oi(e);
  for (let a = -1; ; ) {
    if (o.done && l.len || l.done && o.len)
      throw new Error("Mismatched change set lengths");
    if (o.ins == -1 && l.ins == -1) {
      let h = Math.min(o.len, l.len);
      le(s, h, -1), o.forward(h), l.forward(h);
    } else if (l.ins >= 0 && (o.ins < 0 || a == o.i || o.off == 0 && (l.len < o.len || l.len == o.len && !t))) {
      let h = l.len;
      for (le(s, l.ins, -1); h; ) {
        let c = Math.min(o.len, h);
        o.ins >= 0 && a < o.i && o.len <= c && (le(s, 0, o.ins), r && ft(r, s, o.text), a = o.i), o.forward(c), h -= c;
      }
      l.next();
    } else if (o.ins >= 0) {
      let h = 0, c = o.len;
      for (; c; )
        if (l.ins == -1) {
          let f = Math.min(c, l.len);
          h += f, c -= f, l.forward(f);
        } else if (l.ins == 0 && l.len < c)
          c -= l.len, l.next();
        else
          break;
      le(s, h, a < o.i ? o.ins : 0), r && a < o.i && ft(r, s, o.text), a = o.i, o.forward(o.len - c);
    } else {
      if (o.done && l.done)
        return r ? ee.createSet(s, r) : Xe.create(s);
      throw new Error("Mismatched change set lengths");
    }
  }
}
function Yl(n, e, t = !1) {
  let i = [], s = t ? [] : null, r = new Oi(n), o = new Oi(e);
  for (let l = !1; ; ) {
    if (r.done && o.done)
      return s ? ee.createSet(i, s) : Xe.create(i);
    if (r.ins == 0)
      le(i, r.len, 0, l), r.next();
    else if (o.len == 0 && !o.done)
      le(i, 0, o.ins, l), s && ft(s, i, o.text), o.next();
    else {
      if (r.done || o.done)
        throw new Error("Mismatched change set lengths");
      {
        let a = Math.min(r.len2, o.len), h = i.length;
        if (r.ins == -1) {
          let c = o.ins == -1 ? -1 : o.off ? 0 : o.ins;
          le(i, a, c, l), s && c && ft(s, i, o.text);
        } else o.ins == -1 ? (le(i, r.off ? 0 : r.len, a, l), s && ft(s, i, r.textBit(a))) : (le(i, r.off ? 0 : r.len, o.off ? 0 : o.ins, l), s && !o.off && ft(s, i, o.text));
        l = (r.ins > a || o.ins >= 0 && o.len > a) && (l || i.length > h), r.forward2(a), o.forward(a);
      }
    }
  }
}
class Oi {
  constructor(e) {
    this.set = e, this.i = 0, this.next();
  }
  next() {
    let { sections: e } = this.set;
    this.i < e.length ? (this.len = e[this.i++], this.ins = e[this.i++]) : (this.len = 0, this.ins = -2), this.off = 0;
  }
  get done() {
    return this.ins == -2;
  }
  get len2() {
    return this.ins < 0 ? this.len : this.ins;
  }
  get text() {
    let { inserted: e } = this.set, t = this.i - 2 >> 1;
    return t >= e.length ? $.empty : e[t];
  }
  textBit(e) {
    let { inserted: t } = this.set, i = this.i - 2 >> 1;
    return i >= t.length && !e ? $.empty : t[i].slice(this.off, e == null ? void 0 : this.off + e);
  }
  forward(e) {
    e == this.len ? this.next() : (this.len -= e, this.off += e);
  }
  forward2(e) {
    this.ins == -1 ? this.forward(e) : e == this.ins ? this.next() : (this.ins -= e, this.off += e);
  }
}
class Mt {
  constructor(e, t, i) {
    this.from = e, this.to = t, this.flags = i;
  }
  /**
  The anchor of the range—the side that doesn't move when you
  extend it.
  */
  get anchor() {
    return this.flags & 32 ? this.to : this.from;
  }
  /**
  The head of the range, which is moved when the range is
  [extended](https://codemirror.net/6/docs/ref/#state.SelectionRange.extend).
  */
  get head() {
    return this.flags & 32 ? this.from : this.to;
  }
  /**
  True when `anchor` and `head` are at the same position.
  */
  get empty() {
    return this.from == this.to;
  }
  /**
  If this is a cursor that is explicitly associated with the
  character on one of its sides, this returns the side. -1 means
  the character before its position, 1 the character after, and 0
  means no association.
  */
  get assoc() {
    return this.flags & 8 ? -1 : this.flags & 16 ? 1 : 0;
  }
  /**
  The bidirectional text level associated with this cursor, if
  any.
  */
  get bidiLevel() {
    let e = this.flags & 7;
    return e == 7 ? null : e;
  }
  /**
  The goal column (stored vertical offset) associated with a
  cursor. This is used to preserve the vertical position when
  [moving](https://codemirror.net/6/docs/ref/#view.EditorView.moveVertically) across
  lines of different length.
  */
  get goalColumn() {
    let e = this.flags >> 6;
    return e == 16777215 ? void 0 : e;
  }
  /**
  Map this range through a change, producing a valid range in the
  updated document.
  */
  map(e, t = -1) {
    let i, s;
    return this.empty ? i = s = e.mapPos(this.from, t) : (i = e.mapPos(this.from, 1), s = e.mapPos(this.to, -1)), i == this.from && s == this.to ? this : new Mt(i, s, this.flags);
  }
  /**
  Extend this range to cover at least `from` to `to`.
  */
  extend(e, t = e, i = 0) {
    if (e <= this.anchor && t >= this.anchor)
      return y.range(e, t, void 0, void 0, i);
    let s = Math.abs(e - this.anchor) > Math.abs(t - this.anchor) ? e : t;
    return y.range(this.anchor, s, void 0, void 0, i);
  }
  /**
  Compare this range to another range.
  */
  eq(e, t = !1) {
    return this.anchor == e.anchor && this.head == e.head && this.goalColumn == e.goalColumn && (!t || !this.empty || this.assoc == e.assoc);
  }
  /**
  Return a JSON-serializable object representing the range.
  */
  toJSON() {
    return { anchor: this.anchor, head: this.head };
  }
  /**
  Convert a JSON representation of a range to a `SelectionRange`
  instance.
  */
  static fromJSON(e) {
    if (!e || typeof e.anchor != "number" || typeof e.head != "number")
      throw new RangeError("Invalid JSON representation for SelectionRange");
    return y.range(e.anchor, e.head);
  }
  /**
  @internal
  */
  static create(e, t, i) {
    return new Mt(e, t, i);
  }
}
class y {
  constructor(e, t) {
    this.ranges = e, this.mainIndex = t;
  }
  /**
  Map a selection through a change. Used to adjust the selection
  position for changes.
  */
  map(e, t = -1) {
    return e.empty ? this : y.create(this.ranges.map((i) => i.map(e, t)), this.mainIndex);
  }
  /**
  Compare this selection to another selection. By default, ranges
  are compared only by position. When `includeAssoc` is true,
  cursor ranges must also have the same
  [`assoc`](https://codemirror.net/6/docs/ref/#state.SelectionRange.assoc) value.
  */
  eq(e, t = !1) {
    if (this.ranges.length != e.ranges.length || this.mainIndex != e.mainIndex)
      return !1;
    for (let i = 0; i < this.ranges.length; i++)
      if (!this.ranges[i].eq(e.ranges[i], t))
        return !1;
    return !0;
  }
  /**
  Get the primary selection range. Usually, you should make sure
  your code applies to _all_ ranges, by using methods like
  [`changeByRange`](https://codemirror.net/6/docs/ref/#state.EditorState.changeByRange).
  */
  get main() {
    return this.ranges[this.mainIndex];
  }
  /**
  Make sure the selection only has one range. Returns a selection
  holding only the main range from this selection.
  */
  asSingle() {
    return this.ranges.length == 1 ? this : new y([this.main], 0);
  }
  /**
  Extend this selection with an extra range.
  */
  addRange(e, t = !0) {
    return y.create([e].concat(this.ranges), t ? 0 : this.mainIndex + 1);
  }
  /**
  Replace a given range with another range, and then normalize the
  selection to merge and sort ranges if necessary.
  */
  replaceRange(e, t = this.mainIndex) {
    let i = this.ranges.slice();
    return i[t] = e, y.create(i, this.mainIndex);
  }
  /**
  Convert this selection to an object that can be serialized to
  JSON.
  */
  toJSON() {
    return { ranges: this.ranges.map((e) => e.toJSON()), main: this.mainIndex };
  }
  /**
  Create a selection from a JSON representation.
  */
  static fromJSON(e) {
    if (!e || !Array.isArray(e.ranges) || typeof e.main != "number" || e.main >= e.ranges.length)
      throw new RangeError("Invalid JSON representation for EditorSelection");
    return new y(e.ranges.map((t) => Mt.fromJSON(t)), e.main);
  }
  /**
  Create a selection holding a single range.
  */
  static single(e, t = e) {
    return new y([y.range(e, t)], 0);
  }
  /**
  Sort and merge the given set of ranges, creating a valid
  selection.
  */
  static create(e, t = 0) {
    if (e.length == 0)
      throw new RangeError("A selection needs at least one range");
    for (let i = 0, s = 0; s < e.length; s++) {
      let r = e[s];
      if (r.empty ? r.from <= i : r.from < i)
        return y.normalized(e.slice(), t);
      i = r.to;
    }
    return new y(e, t);
  }
  /**
  Create a cursor selection range at the given position. You can
  safely ignore the optional arguments in most situations.
  */
  static cursor(e, t = 0, i, s) {
    return Mt.create(e, e, (t == 0 ? 0 : t < 0 ? 8 : 16) | (i == null ? 7 : Math.min(6, i)) | (s ?? 16777215) << 6);
  }
  /**
  Create a selection range.
  */
  static range(e, t, i, s, r) {
    let o = (i ?? 16777215) << 6 | (s == null ? 7 : Math.min(6, s));
    return !r && e != t && (r = t < e ? 1 : -1), t < e ? Mt.create(t, e, 48 | o) : Mt.create(e, t, (r ? r < 0 ? 8 : 16 : 0) | o);
  }
  /**
  @internal
  */
  static normalized(e, t = 0) {
    let i = e[t];
    e.sort((s, r) => s.from - r.from), t = e.indexOf(i);
    for (let s = 1; s < e.length; s++) {
      let r = e[s], o = e[s - 1];
      if (r.empty ? r.from <= o.to : r.from < o.to) {
        let l = o.from, a = Math.max(r.to, o.to);
        s <= t && t--, e.splice(--s, 2, r.anchor > r.head ? y.range(a, l) : y.range(l, a));
      }
    }
    return new y(e, t);
  }
}
function Xl(n, e) {
  for (let t of n.ranges)
    if (t.to > e)
      throw new RangeError("Selection points outside of document");
}
let Sr = 0;
class M {
  constructor(e, t, i, s, r) {
    this.combine = e, this.compareInput = t, this.compare = i, this.isStatic = s, this.id = Sr++, this.default = e([]), this.extensions = typeof r == "function" ? r(this) : r;
  }
  /**
  Returns a facet reader for this facet, which can be used to
  [read](https://codemirror.net/6/docs/ref/#state.EditorState.facet) it but not to define values for it.
  */
  get reader() {
    return this;
  }
  /**
  Define a new facet.
  */
  static define(e = {}) {
    return new M(e.combine || ((t) => t), e.compareInput || ((t, i) => t === i), e.compare || (e.combine ? (t, i) => t === i : Ar), !!e.static, e.enables);
  }
  /**
  Returns an extension that adds the given value to this facet.
  */
  of(e) {
    return new yn([], this, 0, e);
  }
  /**
  Create an extension that computes a value for the facet from a
  state. You must take care to declare the parts of the state that
  this value depends on, since your function is only called again
  for a new state when one of those parts changed.
  
  In cases where your value depends only on a single field, you'll
  want to use the [`from`](https://codemirror.net/6/docs/ref/#state.Facet.from) method instead.
  */
  compute(e, t) {
    if (this.isStatic)
      throw new Error("Can't compute a static facet");
    return new yn(e, this, 1, t);
  }
  /**
  Create an extension that computes zero or more values for this
  facet from a state.
  */
  computeN(e, t) {
    if (this.isStatic)
      throw new Error("Can't compute a static facet");
    return new yn(e, this, 2, t);
  }
  from(e, t) {
    return t || (t = (i) => i), this.compute([e], (i) => t(i.field(e)));
  }
}
function Ar(n, e) {
  return n == e || n.length == e.length && n.every((t, i) => t === e[i]);
}
class yn {
  constructor(e, t, i, s) {
    this.dependencies = e, this.facet = t, this.type = i, this.value = s, this.id = Sr++;
  }
  dynamicSlot(e) {
    var t;
    let i = this.value, s = this.facet.compareInput, r = this.id, o = e[r] >> 1, l = this.type == 2, a = !1, h = !1, c = [];
    for (let f of this.dependencies)
      f == "doc" ? a = !0 : f == "selection" ? h = !0 : (((t = e[f.id]) !== null && t !== void 0 ? t : 1) & 1) == 0 && c.push(e[f.id]);
    return {
      create(f) {
        return f.values[o] = i(f), 1;
      },
      update(f, u) {
        if (a && u.docChanged || h && (u.docChanged || u.selection) || $s(f, c)) {
          let d = i(f);
          if (l ? !mo(d, f.values[o], s) : !s(d, f.values[o]))
            return f.values[o] = d, 1;
        }
        return 0;
      },
      reconfigure: (f, u) => {
        let d, p = u.config.address[r];
        if (p != null) {
          let g = Tn(u, p);
          if (this.dependencies.every((m) => m instanceof M ? u.facet(m) === f.facet(m) : m instanceof Ae ? u.field(m, !1) == f.field(m, !1) : !0) || (l ? mo(d = i(f), g, s) : s(d = i(f), g)))
            return f.values[o] = g, 0;
        } else
          d = i(f);
        return f.values[o] = d, 1;
      }
    };
  }
}
function mo(n, e, t) {
  if (n.length != e.length)
    return !1;
  for (let i = 0; i < n.length; i++)
    if (!t(n[i], e[i]))
      return !1;
  return !0;
}
function $s(n, e) {
  let t = !1;
  for (let i of e)
    vi(n, i) & 1 && (t = !0);
  return t;
}
function Yc(n, e, t) {
  let i = t.map((a) => n[a.id]), s = t.map((a) => a.type), r = i.filter((a) => !(a & 1)), o = n[e.id] >> 1;
  function l(a) {
    let h = [];
    for (let c = 0; c < i.length; c++) {
      let f = Tn(a, i[c]);
      if (s[c] == 2)
        for (let u of f)
          h.push(u);
      else
        h.push(f);
    }
    return e.combine(h);
  }
  return {
    create(a) {
      for (let h of i)
        vi(a, h);
      return a.values[o] = l(a), 1;
    },
    update(a, h) {
      if (!$s(a, r))
        return 0;
      let c = l(a);
      return e.compare(c, a.values[o]) ? 0 : (a.values[o] = c, 1);
    },
    reconfigure(a, h) {
      let c = $s(a, i), f = h.config.facets[e.id], u = h.facet(e);
      if (f && !c && Ar(t, f))
        return a.values[o] = u, 0;
      let d = l(a);
      return e.compare(d, u) ? (a.values[o] = u, 0) : (a.values[o] = d, 1);
    }
  };
}
const Yi = /* @__PURE__ */ M.define({ static: !0 });
class Ae {
  constructor(e, t, i, s, r) {
    this.id = e, this.createF = t, this.updateF = i, this.compareF = s, this.spec = r, this.provides = void 0;
  }
  /**
  Define a state field.
  */
  static define(e) {
    let t = new Ae(Sr++, e.create, e.update, e.compare || ((i, s) => i === s), e);
    return e.provide && (t.provides = e.provide(t)), t;
  }
  create(e) {
    let t = e.facet(Yi).find((i) => i.field == this);
    return (t?.create || this.createF)(e);
  }
  /**
  @internal
  */
  slot(e) {
    let t = e[this.id] >> 1;
    return {
      create: (i) => (i.values[t] = this.create(i), 1),
      update: (i, s) => {
        let r = i.values[t], o = this.updateF(r, s);
        return this.compareF(r, o) ? 0 : (i.values[t] = o, 1);
      },
      reconfigure: (i, s) => {
        let r = i.facet(Yi), o = s.facet(Yi), l;
        return (l = r.find((a) => a.field == this)) && l != o.find((a) => a.field == this) ? (i.values[t] = l.create(i), 1) : s.config.address[this.id] != null ? (i.values[t] = s.field(this), 0) : (i.values[t] = this.create(i), 1);
      }
    };
  }
  /**
  Returns an extension that enables this field and overrides the
  way it is initialized. Can be useful when you need to provide a
  non-default starting value for the field.
  */
  init(e) {
    return [this, Yi.of({ field: this, create: e })];
  }
  /**
  State field instances can be used as
  [`Extension`](https://codemirror.net/6/docs/ref/#state.Extension) values to enable the field in a
  given state.
  */
  get extension() {
    return this;
  }
}
const At = { lowest: 4, low: 3, default: 2, high: 1, highest: 0 };
function hi(n) {
  return (e) => new Zl(e, n);
}
const zn = {
  /**
  The highest precedence level, for extensions that should end up
  near the start of the precedence ordering.
  */
  highest: /* @__PURE__ */ hi(At.highest),
  /**
  A higher-than-default precedence, for extensions that should
  come before those with default precedence.
  */
  high: /* @__PURE__ */ hi(At.high),
  /**
  The default precedence, which is also used for extensions
  without an explicit precedence.
  */
  default: /* @__PURE__ */ hi(At.default),
  /**
  A lower-than-default precedence.
  */
  low: /* @__PURE__ */ hi(At.low),
  /**
  The lowest precedence level. Meant for things that should end up
  near the end of the extension order.
  */
  lowest: /* @__PURE__ */ hi(At.lowest)
};
class Zl {
  constructor(e, t) {
    this.inner = e, this.prec = t;
  }
}
class Yt {
  /**
  Create an instance of this compartment to add to your [state
  configuration](https://codemirror.net/6/docs/ref/#state.EditorStateConfig.extensions).
  */
  of(e) {
    return new Ns(this, e);
  }
  /**
  Create an [effect](https://codemirror.net/6/docs/ref/#state.TransactionSpec.effects) that
  reconfigures this compartment.
  */
  reconfigure(e) {
    return Yt.reconfigure.of({ compartment: this, extension: e });
  }
  /**
  Get the current content of the compartment in the state, or
  `undefined` if it isn't present.
  */
  get(e) {
    return e.config.compartments.get(this);
  }
}
class Ns {
  constructor(e, t) {
    this.compartment = e, this.inner = t;
  }
}
class Mn {
  constructor(e, t, i, s, r, o) {
    for (this.base = e, this.compartments = t, this.dynamicSlots = i, this.address = s, this.staticValues = r, this.facets = o, this.statusTemplate = []; this.statusTemplate.length < i.length; )
      this.statusTemplate.push(
        0
        /* SlotStatus.Unresolved */
      );
  }
  staticFacet(e) {
    let t = this.address[e.id];
    return t == null ? e.default : this.staticValues[t >> 1];
  }
  static resolve(e, t, i) {
    let s = [], r = /* @__PURE__ */ Object.create(null), o = /* @__PURE__ */ new Map();
    for (let u of Xc(e, t, o))
      u instanceof Ae ? s.push(u) : (r[u.facet.id] || (r[u.facet.id] = [])).push(u);
    let l = /* @__PURE__ */ Object.create(null), a = [], h = [];
    for (let u of s)
      l[u.id] = h.length << 1, h.push((d) => u.slot(d));
    let c = i?.config.facets;
    for (let u in r) {
      let d = r[u], p = d[0].facet, g = c && c[u] || [];
      if (d.every(
        (m) => m.type == 0
        /* Provider.Static */
      ))
        if (l[p.id] = a.length << 1 | 1, Ar(g, d))
          a.push(i.facet(p));
        else {
          let m = p.combine(d.map((b) => b.value));
          a.push(i && p.compare(m, i.facet(p)) ? i.facet(p) : m);
        }
      else {
        for (let m of d)
          m.type == 0 ? (l[m.id] = a.length << 1 | 1, a.push(m.value)) : (l[m.id] = h.length << 1, h.push((b) => m.dynamicSlot(b)));
        l[p.id] = h.length << 1, h.push((m) => Yc(m, p, d));
      }
    }
    let f = h.map((u) => u(l));
    return new Mn(e, o, f, l, a, r);
  }
}
function Xc(n, e, t) {
  let i = [[], [], [], [], []], s = /* @__PURE__ */ new Map();
  function r(o, l) {
    let a = s.get(o);
    if (a != null) {
      if (a <= l)
        return;
      let h = i[a].indexOf(o);
      h > -1 && i[a].splice(h, 1), o instanceof Ns && t.delete(o.compartment);
    }
    if (s.set(o, l), Array.isArray(o))
      for (let h of o)
        r(h, l);
    else if (o instanceof Ns) {
      if (t.has(o.compartment))
        throw new RangeError("Duplicate use of compartment in extensions");
      let h = e.get(o.compartment) || o.inner;
      t.set(o.compartment, h), r(h, l);
    } else if (o instanceof Zl)
      r(o.inner, o.prec);
    else if (o instanceof Ae)
      i[l].push(o), o.provides && r(o.provides, l);
    else if (o instanceof yn)
      i[l].push(o), o.facet.extensions && r(o.facet.extensions, At.default);
    else {
      let h = o.extension;
      if (!h)
        throw new Error(`Unrecognized extension value in extension set (${o}). This sometimes happens because multiple instances of @codemirror/state are loaded, breaking instanceof checks.`);
      r(h, l);
    }
  }
  return r(n, At.default), i.reduce((o, l) => o.concat(l));
}
function vi(n, e) {
  if (e & 1)
    return 2;
  let t = e >> 1, i = n.status[t];
  if (i == 4)
    throw new Error("Cyclic dependency between fields and/or facets");
  if (i & 2)
    return i;
  n.status[t] = 4;
  let s = n.computeSlot(n, n.config.dynamicSlots[t]);
  return n.status[t] = 2 | s;
}
function Tn(n, e) {
  return e & 1 ? n.config.staticValues[e >> 1] : n.values[e >> 1];
}
const Ql = /* @__PURE__ */ M.define(), Hs = /* @__PURE__ */ M.define({
  combine: (n) => n.some((e) => e),
  static: !0
}), ea = /* @__PURE__ */ M.define({
  combine: (n) => n.length ? n[0] : void 0,
  static: !0
}), ta = /* @__PURE__ */ M.define(), ia = /* @__PURE__ */ M.define(), na = /* @__PURE__ */ M.define(), sa = /* @__PURE__ */ M.define({
  combine: (n) => n.length ? n[0] : !1
});
class yt {
  /**
  @internal
  */
  constructor(e, t) {
    this.type = e, this.value = t;
  }
  /**
  Define a new type of annotation.
  */
  static define() {
    return new Zc();
  }
}
class Zc {
  /**
  Create an instance of this annotation.
  */
  of(e) {
    return new yt(this, e);
  }
}
class Qc {
  /**
  @internal
  */
  constructor(e) {
    this.map = e;
  }
  /**
  Create a [state effect](https://codemirror.net/6/docs/ref/#state.StateEffect) instance of this
  type.
  */
  of(e) {
    return new W(this, e);
  }
}
class W {
  /**
  @internal
  */
  constructor(e, t) {
    this.type = e, this.value = t;
  }
  /**
  Map this effect through a position mapping. Will return
  `undefined` when that ends up deleting the effect.
  */
  map(e) {
    let t = this.type.map(this.value, e);
    return t === void 0 ? void 0 : t == this.value ? this : new W(this.type, t);
  }
  /**
  Tells you whether this effect object is of a given
  [type](https://codemirror.net/6/docs/ref/#state.StateEffectType).
  */
  is(e) {
    return this.type == e;
  }
  /**
  Define a new effect type. The type parameter indicates the type
  of values that his effect holds. It should be a type that
  doesn't include `undefined`, since that is used in
  [mapping](https://codemirror.net/6/docs/ref/#state.StateEffect.map) to indicate that an effect is
  removed.
  */
  static define(e = {}) {
    return new Qc(e.map || ((t) => t));
  }
  /**
  Map an array of effects through a change set.
  */
  static mapEffects(e, t) {
    if (!e.length)
      return e;
    let i = [];
    for (let s of e) {
      let r = s.map(t);
      r && i.push(r);
    }
    return i;
  }
}
W.reconfigure = /* @__PURE__ */ W.define();
W.appendConfig = /* @__PURE__ */ W.define();
class te {
  constructor(e, t, i, s, r, o) {
    this.startState = e, this.changes = t, this.selection = i, this.effects = s, this.annotations = r, this.scrollIntoView = o, this._doc = null, this._state = null, i && Xl(i, t.newLength), r.some((l) => l.type == te.time) || (this.annotations = r.concat(te.time.of(Date.now())));
  }
  /**
  @internal
  */
  static create(e, t, i, s, r, o) {
    return new te(e, t, i, s, r, o);
  }
  /**
  The new document produced by the transaction. Contrary to
  [`.state`](https://codemirror.net/6/docs/ref/#state.Transaction.state)`.doc`, accessing this won't
  force the entire new state to be computed right away, so it is
  recommended that [transaction
  filters](https://codemirror.net/6/docs/ref/#state.EditorState^transactionFilter) use this getter
  when they need to look at the new document.
  */
  get newDoc() {
    return this._doc || (this._doc = this.changes.apply(this.startState.doc));
  }
  /**
  The new selection produced by the transaction. If
  [`this.selection`](https://codemirror.net/6/docs/ref/#state.Transaction.selection) is undefined,
  this will [map](https://codemirror.net/6/docs/ref/#state.EditorSelection.map) the start state's
  current selection through the changes made by the transaction.
  */
  get newSelection() {
    return this.selection || this.startState.selection.map(this.changes);
  }
  /**
  The new state created by the transaction. Computed on demand
  (but retained for subsequent access), so it is recommended not to
  access it in [transaction
  filters](https://codemirror.net/6/docs/ref/#state.EditorState^transactionFilter) when possible.
  */
  get state() {
    return this._state || this.startState.applyTransaction(this), this._state;
  }
  /**
  Get the value of the given annotation type, if any.
  */
  annotation(e) {
    for (let t of this.annotations)
      if (t.type == e)
        return t.value;
  }
  /**
  Indicates whether the transaction changed the document.
  */
  get docChanged() {
    return !this.changes.empty;
  }
  /**
  Indicates whether this transaction reconfigures the state
  (through a [configuration compartment](https://codemirror.net/6/docs/ref/#state.Compartment) or
  with a top-level configuration
  [effect](https://codemirror.net/6/docs/ref/#state.StateEffect^reconfigure).
  */
  get reconfigured() {
    return this.startState.config != this.state.config;
  }
  /**
  Returns true if the transaction has a [user
  event](https://codemirror.net/6/docs/ref/#state.Transaction^userEvent) annotation that is equal to
  or more specific than `event`. For example, if the transaction
  has `"select.pointer"` as user event, `"select"` and
  `"select.pointer"` will match it.
  */
  isUserEvent(e) {
    let t = this.annotation(te.userEvent);
    return !!(t && (t == e || t.length > e.length && t.slice(0, e.length) == e && t[e.length] == "."));
  }
}
te.time = /* @__PURE__ */ yt.define();
te.userEvent = /* @__PURE__ */ yt.define();
te.addToHistory = /* @__PURE__ */ yt.define();
te.remote = /* @__PURE__ */ yt.define();
function ef(n, e) {
  let t = [];
  for (let i = 0, s = 0; ; ) {
    let r, o;
    if (i < n.length && (s == e.length || e[s] >= n[i]))
      r = n[i++], o = n[i++];
    else if (s < e.length)
      r = e[s++], o = e[s++];
    else
      return t;
    !t.length || t[t.length - 1] < r ? t.push(r, o) : t[t.length - 1] < o && (t[t.length - 1] = o);
  }
}
function ra(n, e, t) {
  var i;
  let s, r, o;
  return t ? (s = e.changes, r = ee.empty(e.changes.length), o = n.changes.compose(e.changes)) : (s = e.changes.map(n.changes), r = n.changes.mapDesc(e.changes, !0), o = n.changes.compose(s)), {
    changes: o,
    selection: e.selection ? e.selection.map(r) : (i = n.selection) === null || i === void 0 ? void 0 : i.map(s),
    effects: W.mapEffects(n.effects, s).concat(W.mapEffects(e.effects, r)),
    annotations: n.annotations.length ? n.annotations.concat(e.annotations) : e.annotations,
    scrollIntoView: n.scrollIntoView || e.scrollIntoView
  };
}
function Vs(n, e, t) {
  let i = e.selection, s = jt(e.annotations);
  return e.userEvent && (s = s.concat(te.userEvent.of(e.userEvent))), {
    changes: e.changes instanceof ee ? e.changes : ee.of(e.changes || [], t, n.facet(ea)),
    selection: i && (i instanceof y ? i : y.single(i.anchor, i.head)),
    effects: jt(e.effects),
    annotations: s,
    scrollIntoView: !!e.scrollIntoView
  };
}
function oa(n, e, t) {
  let i = Vs(n, e.length ? e[0] : {}, n.doc.length);
  e.length && e[0].filter === !1 && (t = !1);
  for (let r = 1; r < e.length; r++) {
    e[r].filter === !1 && (t = !1);
    let o = !!e[r].sequential;
    i = ra(i, Vs(n, e[r], o ? i.changes.newLength : n.doc.length), o);
  }
  let s = te.create(n, i.changes, i.selection, i.effects, i.annotations, i.scrollIntoView);
  return nf(t ? tf(s) : s);
}
function tf(n) {
  let e = n.startState, t = !0;
  for (let s of e.facet(ta)) {
    let r = s(n);
    if (r === !1) {
      t = !1;
      break;
    }
    Array.isArray(r) && (t = t === !0 ? r : ef(t, r));
  }
  if (t !== !0) {
    let s, r;
    if (t === !1)
      r = n.changes.invertedDesc, s = ee.empty(e.doc.length);
    else {
      let o = n.changes.filter(t);
      s = o.changes, r = o.filtered.mapDesc(o.changes).invertedDesc;
    }
    n = te.create(e, s, n.selection && n.selection.map(r), W.mapEffects(n.effects, r), n.annotations, n.scrollIntoView);
  }
  let i = e.facet(ia);
  for (let s = i.length - 1; s >= 0; s--) {
    let r = i[s](n);
    r instanceof te ? n = r : Array.isArray(r) && r.length == 1 && r[0] instanceof te ? n = r[0] : n = oa(e, jt(r), !1);
  }
  return n;
}
function nf(n) {
  let e = n.startState, t = e.facet(na), i = n;
  for (let s = t.length - 1; s >= 0; s--) {
    let r = t[s](n);
    r && Object.keys(r).length && (i = ra(i, Vs(e, r, n.changes.newLength), !0));
  }
  return i == n ? n : te.create(e, n.changes, n.selection, i.effects, i.annotations, i.scrollIntoView);
}
const sf = [];
function jt(n) {
  return n == null ? sf : Array.isArray(n) ? n : [n];
}
var tt = /* @__PURE__ */ (function(n) {
  return n[n.Word = 0] = "Word", n[n.Space = 1] = "Space", n[n.Other = 2] = "Other", n;
})(tt || (tt = {}));
const rf = /[\u00df\u0587\u0590-\u05f4\u0600-\u06ff\u3040-\u309f\u30a0-\u30ff\u3400-\u4db5\u4e00-\u9fcc\uac00-\ud7af]/;
let Fs;
try {
  Fs = /* @__PURE__ */ new RegExp("[\\p{Alphabetic}\\p{Number}_]", "u");
} catch {
}
function of(n) {
  if (Fs)
    return Fs.test(n);
  for (let e = 0; e < n.length; e++) {
    let t = n[e];
    if (/\w/.test(t) || t > "" && (t.toUpperCase() != t.toLowerCase() || rf.test(t)))
      return !0;
  }
  return !1;
}
function lf(n) {
  return (e) => {
    if (!/\S/.test(e))
      return tt.Space;
    if (of(e))
      return tt.Word;
    for (let t = 0; t < n.length; t++)
      if (e.indexOf(n[t]) > -1)
        return tt.Word;
    return tt.Other;
  };
}
class N {
  constructor(e, t, i, s, r, o) {
    this.config = e, this.doc = t, this.selection = i, this.values = s, this.status = e.statusTemplate.slice(), this.computeSlot = r, o && (o._state = this);
    for (let l = 0; l < this.config.dynamicSlots.length; l++)
      vi(this, l << 1);
    this.computeSlot = null;
  }
  field(e, t = !0) {
    let i = this.config.address[e.id];
    if (i == null) {
      if (t)
        throw new RangeError("Field is not present in this state");
      return;
    }
    return vi(this, i), Tn(this, i);
  }
  /**
  Create a [transaction](https://codemirror.net/6/docs/ref/#state.Transaction) that updates this
  state. Any number of [transaction specs](https://codemirror.net/6/docs/ref/#state.TransactionSpec)
  can be passed. Unless
  [`sequential`](https://codemirror.net/6/docs/ref/#state.TransactionSpec.sequential) is set, the
  [changes](https://codemirror.net/6/docs/ref/#state.TransactionSpec.changes) (if any) of each spec
  are assumed to start in the _current_ document (not the document
  produced by previous specs), and its
  [selection](https://codemirror.net/6/docs/ref/#state.TransactionSpec.selection) and
  [effects](https://codemirror.net/6/docs/ref/#state.TransactionSpec.effects) are assumed to refer
  to the document created by its _own_ changes. The resulting
  transaction contains the combined effect of all the different
  specs. For [selection](https://codemirror.net/6/docs/ref/#state.TransactionSpec.selection), later
  specs take precedence over earlier ones.
  */
  update(...e) {
    return oa(this, e, !0);
  }
  /**
  @internal
  */
  applyTransaction(e) {
    let t = this.config, { base: i, compartments: s } = t;
    for (let l of e.effects)
      l.is(Yt.reconfigure) ? (t && (s = /* @__PURE__ */ new Map(), t.compartments.forEach((a, h) => s.set(h, a)), t = null), s.set(l.value.compartment, l.value.extension)) : l.is(W.reconfigure) ? (t = null, i = l.value) : l.is(W.appendConfig) && (t = null, i = jt(i).concat(l.value));
    let r;
    t ? r = e.startState.values.slice() : (t = Mn.resolve(i, s, this), r = new N(t, this.doc, this.selection, t.dynamicSlots.map(() => null), (a, h) => h.reconfigure(a, this), null).values);
    let o = e.startState.facet(Hs) ? e.newSelection : e.newSelection.asSingle();
    new N(t, e.newDoc, o, r, (l, a) => a.update(l, e), e);
  }
  /**
  Create a [transaction spec](https://codemirror.net/6/docs/ref/#state.TransactionSpec) that
  replaces every selection range with the given content.
  */
  replaceSelection(e) {
    return typeof e == "string" && (e = this.toText(e)), this.changeByRange((t) => ({
      changes: { from: t.from, to: t.to, insert: e },
      range: y.cursor(t.from + e.length)
    }));
  }
  /**
  Create a set of changes and a new selection by running the given
  function for each range in the active selection. The function
  can return an optional set of changes (in the coordinate space
  of the start document), plus an updated range (in the coordinate
  space of the document produced by the call's own changes). This
  method will merge all the changes and ranges into a single
  changeset and selection, and return it as a [transaction
  spec](https://codemirror.net/6/docs/ref/#state.TransactionSpec), which can be passed to
  [`update`](https://codemirror.net/6/docs/ref/#state.EditorState.update).
  */
  changeByRange(e) {
    let t = this.selection, i = e(t.ranges[0]), s = this.changes(i.changes), r = [i.range], o = jt(i.effects);
    for (let l = 1; l < t.ranges.length; l++) {
      let a = e(t.ranges[l]), h = this.changes(a.changes), c = h.map(s);
      for (let u = 0; u < l; u++)
        r[u] = r[u].map(c);
      let f = s.mapDesc(h, !0);
      r.push(a.range.map(f)), s = s.compose(c), o = W.mapEffects(o, c).concat(W.mapEffects(jt(a.effects), f));
    }
    return {
      changes: s,
      selection: y.create(r, t.mainIndex),
      effects: o
    };
  }
  /**
  Create a [change set](https://codemirror.net/6/docs/ref/#state.ChangeSet) from the given change
  description, taking the state's document length and line
  separator into account.
  */
  changes(e = []) {
    return e instanceof ee ? e : ee.of(e, this.doc.length, this.facet(N.lineSeparator));
  }
  /**
  Using the state's [line
  separator](https://codemirror.net/6/docs/ref/#state.EditorState^lineSeparator), create a
  [`Text`](https://codemirror.net/6/docs/ref/#state.Text) instance from the given string.
  */
  toText(e) {
    return $.of(e.split(this.facet(N.lineSeparator) || Bs));
  }
  /**
  Return the given range of the document as a string.
  */
  sliceDoc(e = 0, t = this.doc.length) {
    return this.doc.sliceString(e, t, this.lineBreak);
  }
  /**
  Get the value of a state [facet](https://codemirror.net/6/docs/ref/#state.Facet).
  */
  facet(e) {
    let t = this.config.address[e.id];
    return t == null ? e.default : (vi(this, t), Tn(this, t));
  }
  /**
  Convert this state to a JSON-serializable object. When custom
  fields should be serialized, you can pass them in as an object
  mapping property names (in the resulting object, which should
  not use `doc` or `selection`) to fields.
  */
  toJSON(e) {
    let t = {
      doc: this.sliceDoc(),
      selection: this.selection.toJSON()
    };
    if (e)
      for (let i in e) {
        let s = e[i];
        s instanceof Ae && this.config.address[s.id] != null && (t[i] = s.spec.toJSON(this.field(e[i]), this));
      }
    return t;
  }
  /**
  Deserialize a state from its JSON representation. When custom
  fields should be deserialized, pass the same object you passed
  to [`toJSON`](https://codemirror.net/6/docs/ref/#state.EditorState.toJSON) when serializing as
  third argument.
  */
  static fromJSON(e, t = {}, i) {
    if (!e || typeof e.doc != "string")
      throw new RangeError("Invalid JSON representation for EditorState");
    let s = [];
    if (i) {
      for (let r in i)
        if (Object.prototype.hasOwnProperty.call(e, r)) {
          let o = i[r], l = e[r];
          s.push(o.init((a) => o.spec.fromJSON(l, a)));
        }
    }
    return N.create({
      doc: e.doc,
      selection: y.fromJSON(e.selection),
      extensions: t.extensions ? s.concat([t.extensions]) : s
    });
  }
  /**
  Create a new state. You'll usually only need this when
  initializing an editor—updated states are created by applying
  transactions.
  */
  static create(e = {}) {
    let t = Mn.resolve(e.extensions || [], /* @__PURE__ */ new Map()), i = e.doc instanceof $ ? e.doc : $.of((e.doc || "").split(t.staticFacet(N.lineSeparator) || Bs)), s = e.selection ? e.selection instanceof y ? e.selection : y.single(e.selection.anchor, e.selection.head) : y.single(0);
    return Xl(s, i.length), t.staticFacet(Hs) || (s = s.asSingle()), new N(t, i, s, t.dynamicSlots.map(() => null), (r, o) => o.create(r), null);
  }
  /**
  The size (in columns) of a tab in the document, determined by
  the [`tabSize`](https://codemirror.net/6/docs/ref/#state.EditorState^tabSize) facet.
  */
  get tabSize() {
    return this.facet(N.tabSize);
  }
  /**
  Get the proper [line-break](https://codemirror.net/6/docs/ref/#state.EditorState^lineSeparator)
  string for this state.
  */
  get lineBreak() {
    return this.facet(N.lineSeparator) || `
`;
  }
  /**
  Returns true when the editor is
  [configured](https://codemirror.net/6/docs/ref/#state.EditorState^readOnly) to be read-only.
  */
  get readOnly() {
    return this.facet(sa);
  }
  /**
  Look up a translation for the given phrase (via the
  [`phrases`](https://codemirror.net/6/docs/ref/#state.EditorState^phrases) facet), or return the
  original string if no translation is found.
  
  If additional arguments are passed, they will be inserted in
  place of markers like `$1` (for the first value) and `$2`, etc.
  A single `$` is equivalent to `$1`, and `$$` will produce a
  literal dollar sign.
  */
  phrase(e, ...t) {
    for (let i of this.facet(N.phrases))
      if (Object.prototype.hasOwnProperty.call(i, e)) {
        e = i[e];
        break;
      }
    return t.length && (e = e.replace(/\$(\$|\d*)/g, (i, s) => {
      if (s == "$")
        return "$";
      let r = +(s || 1);
      return !r || r > t.length ? i : t[r - 1];
    })), e;
  }
  /**
  Find the values for a given language data field, provided by the
  the [`languageData`](https://codemirror.net/6/docs/ref/#state.EditorState^languageData) facet.
  
  Examples of language data fields are...
  
  - [`"commentTokens"`](https://codemirror.net/6/docs/ref/#commands.CommentTokens) for specifying
    comment syntax.
  - [`"autocomplete"`](https://codemirror.net/6/docs/ref/#autocomplete.autocompletion^config.override)
    for providing language-specific completion sources.
  - [`"wordChars"`](https://codemirror.net/6/docs/ref/#state.EditorState.charCategorizer) for adding
    characters that should be considered part of words in this
    language.
  - [`"closeBrackets"`](https://codemirror.net/6/docs/ref/#autocomplete.CloseBracketConfig) controls
    bracket closing behavior.
  */
  languageDataAt(e, t, i = -1) {
    let s = [];
    for (let r of this.facet(Ql))
      for (let o of r(this, t, i))
        Object.prototype.hasOwnProperty.call(o, e) && s.push(o[e]);
    return s;
  }
  /**
  Return a function that can categorize strings (expected to
  represent a single [grapheme cluster](https://codemirror.net/6/docs/ref/#state.findClusterBreak))
  into one of:
  
   - Word (contains an alphanumeric character or a character
     explicitly listed in the local language's `"wordChars"`
     language data, which should be a string)
   - Space (contains only whitespace)
   - Other (anything else)
  */
  charCategorizer(e) {
    let t = this.languageDataAt("wordChars", e);
    return lf(t.length ? t[0] : "");
  }
  /**
  Find the word at the given position, meaning the range
  containing all [word](https://codemirror.net/6/docs/ref/#state.CharCategory.Word) characters
  around it. If no word characters are adjacent to the position,
  this returns null.
  */
  wordAt(e) {
    let { text: t, from: i, length: s } = this.doc.lineAt(e), r = this.charCategorizer(e), o = e - i, l = e - i;
    for (; o > 0; ) {
      let a = ae(t, o, !1);
      if (r(t.slice(a, o)) != tt.Word)
        break;
      o = a;
    }
    for (; l < s; ) {
      let a = ae(t, l);
      if (r(t.slice(l, a)) != tt.Word)
        break;
      l = a;
    }
    return o == l ? null : y.range(o + i, l + i);
  }
}
N.allowMultipleSelections = Hs;
N.tabSize = /* @__PURE__ */ M.define({
  combine: (n) => n.length ? n[0] : 4
});
N.lineSeparator = ea;
N.readOnly = sa;
N.phrases = /* @__PURE__ */ M.define({
  compare(n, e) {
    let t = Object.keys(n), i = Object.keys(e);
    return t.length == i.length && t.every((s) => n[s] == e[s]);
  }
});
N.languageData = Ql;
N.changeFilter = ta;
N.transactionFilter = ia;
N.transactionExtender = na;
Yt.reconfigure = /* @__PURE__ */ W.define();
function _i(n, e, t = {}) {
  let i = {};
  for (let s of n)
    for (let r of Object.keys(s)) {
      let o = s[r], l = i[r];
      if (l === void 0)
        i[r] = o;
      else if (!(l === o || o === void 0)) if (Object.hasOwnProperty.call(t, r))
        i[r] = t[r](l, o);
      else
        throw new Error("Config merge conflict for field " + r);
    }
  for (let s in e)
    i[s] === void 0 && (i[s] = e[s]);
  return i;
}
class Rt {
  /**
  Compare this value with another value. Used when comparing
  rangesets. The default implementation compares by identity.
  Unless you are only creating a fixed number of unique instances
  of your value type, it is a good idea to implement this
  properly.
  */
  eq(e) {
    return this == e;
  }
  /**
  Create a [range](https://codemirror.net/6/docs/ref/#state.Range) with this value.
  */
  range(e, t = e) {
    return Ws.create(e, t, this);
  }
}
Rt.prototype.startSide = Rt.prototype.endSide = 0;
Rt.prototype.point = !1;
Rt.prototype.mapMode = pe.TrackDel;
function Cr(n, e) {
  return n == e || n.constructor == e.constructor && n.eq(e);
}
let Ws = class la {
  constructor(e, t, i) {
    this.from = e, this.to = t, this.value = i;
  }
  /**
  @internal
  */
  static create(e, t, i) {
    return new la(e, t, i);
  }
};
function _s(n, e) {
  return n.from - e.from || n.value.startSide - e.value.startSide;
}
class Mr {
  constructor(e, t, i, s) {
    this.from = e, this.to = t, this.value = i, this.maxPoint = s;
  }
  get length() {
    return this.to[this.to.length - 1];
  }
  // Find the index of the given position and side. Use the ranges'
  // `from` pos when `end == false`, `to` when `end == true`.
  findIndex(e, t, i, s = 0) {
    let r = i ? this.to : this.from;
    for (let o = s, l = r.length; ; ) {
      if (o == l)
        return o;
      let a = o + l >> 1, h = r[a] - e || (i ? this.value[a].endSide : this.value[a].startSide) - t;
      if (a == o)
        return h >= 0 ? o : l;
      h >= 0 ? l = a : o = a + 1;
    }
  }
  between(e, t, i, s) {
    for (let r = this.findIndex(t, -1e9, !0), o = this.findIndex(i, 1e9, !1, r); r < o; r++)
      if (s(this.from[r] + e, this.to[r] + e, this.value[r]) === !1)
        return !1;
  }
  map(e, t) {
    let i = [], s = [], r = [], o = -1, l = -1;
    for (let a = 0; a < this.value.length; a++) {
      let h = this.value[a], c = this.from[a] + e, f = this.to[a] + e, u, d;
      if (c == f) {
        let p = t.mapPos(c, h.startSide, h.mapMode);
        if (p == null || (u = d = p, h.startSide != h.endSide && (d = t.mapPos(c, h.endSide), d < u)))
          continue;
      } else if (u = t.mapPos(c, h.startSide), d = t.mapPos(f, h.endSide), u > d || u == d && h.startSide > 0 && h.endSide <= 0)
        continue;
      (d - u || h.endSide - h.startSide) < 0 || (o < 0 && (o = u), h.point && (l = Math.max(l, d - u)), i.push(h), s.push(u - o), r.push(d - o));
    }
    return { mapped: i.length ? new Mr(s, r, i, l) : null, pos: o };
  }
}
class B {
  constructor(e, t, i, s) {
    this.chunkPos = e, this.chunk = t, this.nextLayer = i, this.maxPoint = s;
  }
  /**
  @internal
  */
  static create(e, t, i, s) {
    return new B(e, t, i, s);
  }
  /**
  @internal
  */
  get length() {
    let e = this.chunk.length - 1;
    return e < 0 ? 0 : Math.max(this.chunkEnd(e), this.nextLayer.length);
  }
  /**
  The number of ranges in the set.
  */
  get size() {
    if (this.isEmpty)
      return 0;
    let e = this.nextLayer.size;
    for (let t of this.chunk)
      e += t.value.length;
    return e;
  }
  /**
  @internal
  */
  chunkEnd(e) {
    return this.chunkPos[e] + this.chunk[e].length;
  }
  /**
  Update the range set, optionally adding new ranges or filtering
  out existing ones.
  
  (Note: The type parameter is just there as a kludge to work
  around TypeScript variance issues that prevented `RangeSet<X>`
  from being a subtype of `RangeSet<Y>` when `X` is a subtype of
  `Y`.)
  */
  update(e) {
    let { add: t = [], sort: i = !1, filterFrom: s = 0, filterTo: r = this.length } = e, o = e.filter;
    if (t.length == 0 && !o)
      return this;
    if (i && (t = t.slice().sort(_s)), this.isEmpty)
      return t.length ? B.of(t) : this;
    let l = new aa(this, null, -1).goto(0), a = 0, h = [], c = new Xt();
    for (; l.value || a < t.length; )
      if (a < t.length && (l.from - t[a].from || l.startSide - t[a].value.startSide) >= 0) {
        let f = t[a++];
        c.addInner(f.from, f.to, f.value) || h.push(f);
      } else l.rangeIndex == 1 && l.chunkIndex < this.chunk.length && (a == t.length || this.chunkEnd(l.chunkIndex) < t[a].from) && (!o || s > this.chunkEnd(l.chunkIndex) || r < this.chunkPos[l.chunkIndex]) && c.addChunk(this.chunkPos[l.chunkIndex], this.chunk[l.chunkIndex]) ? l.nextChunk() : ((!o || s > l.to || r < l.from || o(l.from, l.to, l.value)) && (c.addInner(l.from, l.to, l.value) || h.push(Ws.create(l.from, l.to, l.value))), l.next());
    return c.finishInner(this.nextLayer.isEmpty && !h.length ? B.empty : this.nextLayer.update({ add: h, filter: o, filterFrom: s, filterTo: r }));
  }
  /**
  Map this range set through a set of changes, return the new set.
  */
  map(e) {
    if (e.empty || this.isEmpty)
      return this;
    let t = [], i = [], s = -1;
    for (let o = 0; o < this.chunk.length; o++) {
      let l = this.chunkPos[o], a = this.chunk[o], h = e.touchesRange(l, l + a.length);
      if (h === !1)
        s = Math.max(s, a.maxPoint), t.push(a), i.push(e.mapPos(l));
      else if (h === !0) {
        let { mapped: c, pos: f } = a.map(l, e);
        c && (s = Math.max(s, c.maxPoint), t.push(c), i.push(f));
      }
    }
    let r = this.nextLayer.map(e);
    return t.length == 0 ? r : new B(i, t, r || B.empty, s);
  }
  /**
  Iterate over the ranges that touch the region `from` to `to`,
  calling `f` for each. There is no guarantee that the ranges will
  be reported in any specific order. When the callback returns
  `false`, iteration stops.
  */
  between(e, t, i) {
    if (!this.isEmpty) {
      for (let s = 0; s < this.chunk.length; s++) {
        let r = this.chunkPos[s], o = this.chunk[s];
        if (t >= r && e <= r + o.length && o.between(r, e - r, t - r, i) === !1)
          return;
      }
      this.nextLayer.between(e, t, i);
    }
  }
  /**
  Iterate over the ranges in this set, in order, including all
  ranges that end at or after `from`.
  */
  iter(e = 0) {
    return Di.from([this]).goto(e);
  }
  /**
  @internal
  */
  get isEmpty() {
    return this.nextLayer == this;
  }
  /**
  Iterate over the ranges in a collection of sets, in order,
  starting from `from`.
  */
  static iter(e, t = 0) {
    return Di.from(e).goto(t);
  }
  /**
  Iterate over two groups of sets, calling methods on `comparator`
  to notify it of possible differences.
  */
  static compare(e, t, i, s, r = -1) {
    let o = e.filter((f) => f.maxPoint > 0 || !f.isEmpty && f.maxPoint >= r), l = t.filter((f) => f.maxPoint > 0 || !f.isEmpty && f.maxPoint >= r), a = bo(o, l, i), h = new ci(o, a, r), c = new ci(l, a, r);
    i.iterGaps((f, u, d) => yo(h, f, c, u, d, s)), i.empty && i.length == 0 && yo(h, 0, c, 0, 0, s);
  }
  /**
  Compare the contents of two groups of range sets, returning true
  if they are equivalent in the given range.
  */
  static eq(e, t, i = 0, s) {
    s == null && (s = 999999999);
    let r = e.filter((c) => !c.isEmpty && t.indexOf(c) < 0), o = t.filter((c) => !c.isEmpty && e.indexOf(c) < 0);
    if (r.length != o.length)
      return !1;
    if (!r.length)
      return !0;
    let l = bo(r, o), a = new ci(r, l, 0).goto(i), h = new ci(o, l, 0).goto(i);
    for (; ; ) {
      if (a.to != h.to || !zs(a.active, h.active) || a.point && (!h.point || !Cr(a.point, h.point)))
        return !1;
      if (a.to > s)
        return !0;
      a.next(), h.next();
    }
  }
  /**
  Iterate over a group of range sets at the same time, notifying
  the iterator about the ranges covering every given piece of
  content. Returns the open count (see
  [`SpanIterator.span`](https://codemirror.net/6/docs/ref/#state.SpanIterator.span)) at the end
  of the iteration.
  */
  static spans(e, t, i, s, r = -1) {
    let o = new ci(e, null, r).goto(t), l = t, a = o.openStart;
    for (; ; ) {
      let h = Math.min(o.to, i);
      if (o.point) {
        let c = o.activeForPoint(o.to), f = o.pointFrom < t ? c.length + 1 : o.point.startSide < 0 ? c.length : Math.min(c.length, a);
        s.point(l, h, o.point, c, f, o.pointRank), a = Math.min(o.openEnd(h), c.length);
      } else h > l && (s.span(l, h, o.active, a), a = o.openEnd(h));
      if (o.to > i)
        return a + (o.point && o.to > i ? 1 : 0);
      l = o.to, o.next();
    }
  }
  /**
  Create a range set for the given range or array of ranges. By
  default, this expects the ranges to be _sorted_ (by start
  position and, if two start at the same position,
  `value.startSide`). You can pass `true` as second argument to
  cause the method to sort them.
  */
  static of(e, t = !1) {
    let i = new Xt();
    for (let s of e instanceof Ws ? [e] : t ? af(e) : e)
      i.add(s.from, s.to, s.value);
    return i.finish();
  }
  /**
  Join an array of range sets into a single set.
  */
  static join(e) {
    if (!e.length)
      return B.empty;
    let t = e[e.length - 1];
    for (let i = e.length - 2; i >= 0; i--)
      for (let s = e[i]; s != B.empty; s = s.nextLayer)
        t = new B(s.chunkPos, s.chunk, t, Math.max(s.maxPoint, t.maxPoint));
    return t;
  }
}
B.empty = /* @__PURE__ */ new B([], [], null, -1);
function af(n) {
  if (n.length > 1)
    for (let e = n[0], t = 1; t < n.length; t++) {
      let i = n[t];
      if (_s(e, i) > 0)
        return n.slice().sort(_s);
      e = i;
    }
  return n;
}
B.empty.nextLayer = B.empty;
class Xt {
  finishChunk(e) {
    this.chunks.push(new Mr(this.from, this.to, this.value, this.maxPoint)), this.chunkPos.push(this.chunkStart), this.chunkStart = -1, this.setMaxPoint = Math.max(this.setMaxPoint, this.maxPoint), this.maxPoint = -1, e && (this.from = [], this.to = [], this.value = []);
  }
  /**
  Create an empty builder.
  */
  constructor() {
    this.chunks = [], this.chunkPos = [], this.chunkStart = -1, this.last = null, this.lastFrom = -1e9, this.lastTo = -1e9, this.from = [], this.to = [], this.value = [], this.maxPoint = -1, this.setMaxPoint = -1, this.nextLayer = null;
  }
  /**
  Add a range. Ranges should be added in sorted (by `from` and
  `value.startSide`) order.
  */
  add(e, t, i) {
    this.addInner(e, t, i) || (this.nextLayer || (this.nextLayer = new Xt())).add(e, t, i);
  }
  /**
  @internal
  */
  addInner(e, t, i) {
    let s = e - this.lastTo || i.startSide - this.last.endSide;
    if (s <= 0 && (e - this.lastFrom || i.startSide - this.last.startSide) < 0)
      throw new Error("Ranges must be added sorted by `from` position and `startSide`");
    return s < 0 ? !1 : (this.from.length == 250 && this.finishChunk(!0), this.chunkStart < 0 && (this.chunkStart = e), this.from.push(e - this.chunkStart), this.to.push(t - this.chunkStart), this.last = i, this.lastFrom = e, this.lastTo = t, this.value.push(i), i.point && (this.maxPoint = Math.max(this.maxPoint, t - e)), !0);
  }
  /**
  @internal
  */
  addChunk(e, t) {
    if ((e - this.lastTo || t.value[0].startSide - this.last.endSide) < 0)
      return !1;
    this.from.length && this.finishChunk(!0), this.setMaxPoint = Math.max(this.setMaxPoint, t.maxPoint), this.chunks.push(t), this.chunkPos.push(e);
    let i = t.value.length - 1;
    return this.last = t.value[i], this.lastFrom = t.from[i] + e, this.lastTo = t.to[i] + e, !0;
  }
  /**
  Finish the range set. Returns the new set. The builder can't be
  used anymore after this has been called.
  */
  finish() {
    return this.finishInner(B.empty);
  }
  /**
  @internal
  */
  finishInner(e) {
    if (this.from.length && this.finishChunk(!1), this.chunks.length == 0)
      return e;
    let t = B.create(this.chunkPos, this.chunks, this.nextLayer ? this.nextLayer.finishInner(e) : e, this.setMaxPoint);
    return this.from = null, t;
  }
}
function bo(n, e, t) {
  let i = /* @__PURE__ */ new Map();
  for (let r of n)
    for (let o = 0; o < r.chunk.length; o++)
      r.chunk[o].maxPoint <= 0 && i.set(r.chunk[o], r.chunkPos[o]);
  let s = /* @__PURE__ */ new Set();
  for (let r of e)
    for (let o = 0; o < r.chunk.length; o++) {
      let l = i.get(r.chunk[o]);
      l != null && (t ? t.mapPos(l) : l) == r.chunkPos[o] && !t?.touchesRange(l, l + r.chunk[o].length) && s.add(r.chunk[o]);
    }
  return s;
}
class aa {
  constructor(e, t, i, s = 0) {
    this.layer = e, this.skip = t, this.minPoint = i, this.rank = s;
  }
  get startSide() {
    return this.value ? this.value.startSide : 0;
  }
  get endSide() {
    return this.value ? this.value.endSide : 0;
  }
  goto(e, t = -1e9) {
    return this.chunkIndex = this.rangeIndex = 0, this.gotoInner(e, t, !1), this;
  }
  gotoInner(e, t, i) {
    for (; this.chunkIndex < this.layer.chunk.length; ) {
      let s = this.layer.chunk[this.chunkIndex];
      if (!(this.skip && this.skip.has(s) || this.layer.chunkEnd(this.chunkIndex) < e || s.maxPoint < this.minPoint))
        break;
      this.chunkIndex++, i = !1;
    }
    if (this.chunkIndex < this.layer.chunk.length) {
      let s = this.layer.chunk[this.chunkIndex].findIndex(e - this.layer.chunkPos[this.chunkIndex], t, !0);
      (!i || this.rangeIndex < s) && this.setRangeIndex(s);
    }
    this.next();
  }
  forward(e, t) {
    (this.to - e || this.endSide - t) < 0 && this.gotoInner(e, t, !0);
  }
  next() {
    for (; ; )
      if (this.chunkIndex == this.layer.chunk.length) {
        this.from = this.to = 1e9, this.value = null;
        break;
      } else {
        let e = this.layer.chunkPos[this.chunkIndex], t = this.layer.chunk[this.chunkIndex], i = e + t.from[this.rangeIndex];
        if (this.from = i, this.to = e + t.to[this.rangeIndex], this.value = t.value[this.rangeIndex], this.setRangeIndex(this.rangeIndex + 1), this.minPoint < 0 || this.value.point && this.to - this.from >= this.minPoint)
          break;
      }
  }
  setRangeIndex(e) {
    if (e == this.layer.chunk[this.chunkIndex].value.length) {
      if (this.chunkIndex++, this.skip)
        for (; this.chunkIndex < this.layer.chunk.length && this.skip.has(this.layer.chunk[this.chunkIndex]); )
          this.chunkIndex++;
      this.rangeIndex = 0;
    } else
      this.rangeIndex = e;
  }
  nextChunk() {
    this.chunkIndex++, this.rangeIndex = 0, this.next();
  }
  compare(e) {
    return this.from - e.from || this.startSide - e.startSide || this.rank - e.rank || this.to - e.to || this.endSide - e.endSide;
  }
}
class Di {
  constructor(e) {
    this.heap = e;
  }
  static from(e, t = null, i = -1) {
    let s = [];
    for (let r = 0; r < e.length; r++)
      for (let o = e[r]; !o.isEmpty; o = o.nextLayer)
        o.maxPoint >= i && s.push(new aa(o, t, i, r));
    return s.length == 1 ? s[0] : new Di(s);
  }
  get startSide() {
    return this.value ? this.value.startSide : 0;
  }
  goto(e, t = -1e9) {
    for (let i of this.heap)
      i.goto(e, t);
    for (let i = this.heap.length >> 1; i >= 0; i--)
      hs(this.heap, i);
    return this.next(), this;
  }
  forward(e, t) {
    for (let i of this.heap)
      i.forward(e, t);
    for (let i = this.heap.length >> 1; i >= 0; i--)
      hs(this.heap, i);
    (this.to - e || this.value.endSide - t) < 0 && this.next();
  }
  next() {
    if (this.heap.length == 0)
      this.from = this.to = 1e9, this.value = null, this.rank = -1;
    else {
      let e = this.heap[0];
      this.from = e.from, this.to = e.to, this.value = e.value, this.rank = e.rank, e.value && e.next(), hs(this.heap, 0);
    }
  }
}
function hs(n, e) {
  for (let t = n[e]; ; ) {
    let i = (e << 1) + 1;
    if (i >= n.length)
      break;
    let s = n[i];
    if (i + 1 < n.length && s.compare(n[i + 1]) >= 0 && (s = n[i + 1], i++), t.compare(s) < 0)
      break;
    n[i] = t, n[e] = s, e = i;
  }
}
class ci {
  constructor(e, t, i) {
    this.minPoint = i, this.active = [], this.activeTo = [], this.activeRank = [], this.minActive = -1, this.point = null, this.pointFrom = 0, this.pointRank = 0, this.to = -1e9, this.endSide = 0, this.openStart = -1, this.cursor = Di.from(e, t, i);
  }
  goto(e, t = -1e9) {
    return this.cursor.goto(e, t), this.active.length = this.activeTo.length = this.activeRank.length = 0, this.minActive = -1, this.to = e, this.endSide = t, this.openStart = -1, this.next(), this;
  }
  forward(e, t) {
    for (; this.minActive > -1 && (this.activeTo[this.minActive] - e || this.active[this.minActive].endSide - t) < 0; )
      this.removeActive(this.minActive);
    this.cursor.forward(e, t);
  }
  removeActive(e) {
    Xi(this.active, e), Xi(this.activeTo, e), Xi(this.activeRank, e), this.minActive = wo(this.active, this.activeTo);
  }
  addActive(e) {
    let t = 0, { value: i, to: s, rank: r } = this.cursor;
    for (; t < this.activeRank.length && (r - this.activeRank[t] || s - this.activeTo[t]) > 0; )
      t++;
    Zi(this.active, t, i), Zi(this.activeTo, t, s), Zi(this.activeRank, t, r), e && Zi(e, t, this.cursor.from), this.minActive = wo(this.active, this.activeTo);
  }
  // After calling this, if `this.point` != null, the next range is a
  // point. Otherwise, it's a regular range, covered by `this.active`.
  next() {
    let e = this.to, t = this.point;
    this.point = null;
    let i = this.openStart < 0 ? [] : null;
    for (; ; ) {
      let s = this.minActive;
      if (s > -1 && (this.activeTo[s] - this.cursor.from || this.active[s].endSide - this.cursor.startSide) < 0) {
        if (this.activeTo[s] > e) {
          this.to = this.activeTo[s], this.endSide = this.active[s].endSide;
          break;
        }
        this.removeActive(s), i && Xi(i, s);
      } else if (this.cursor.value)
        if (this.cursor.from > e) {
          this.to = this.cursor.from, this.endSide = this.cursor.startSide;
          break;
        } else {
          let r = this.cursor.value;
          if (!r.point)
            this.addActive(i), this.cursor.next();
          else if (t && this.cursor.to == this.to && this.cursor.from < this.cursor.to)
            this.cursor.next();
          else {
            this.point = r, this.pointFrom = this.cursor.from, this.pointRank = this.cursor.rank, this.to = this.cursor.to, this.endSide = r.endSide, this.cursor.next(), this.forward(this.to, this.endSide);
            break;
          }
        }
      else {
        this.to = this.endSide = 1e9;
        break;
      }
    }
    if (i) {
      this.openStart = 0;
      for (let s = i.length - 1; s >= 0 && i[s] < e; s--)
        this.openStart++;
    }
  }
  activeForPoint(e) {
    if (!this.active.length)
      return this.active;
    let t = [];
    for (let i = this.active.length - 1; i >= 0 && !(this.activeRank[i] < this.pointRank); i--)
      (this.activeTo[i] > e || this.activeTo[i] == e && this.active[i].endSide >= this.point.endSide) && t.push(this.active[i]);
    return t.reverse();
  }
  openEnd(e) {
    let t = 0;
    for (let i = this.activeTo.length - 1; i >= 0 && this.activeTo[i] > e; i--)
      t++;
    return t;
  }
}
function yo(n, e, t, i, s, r) {
  n.goto(e), t.goto(i);
  let o = i + s, l = i, a = i - e, h = !!r.boundChange;
  for (let c = !1; ; ) {
    let f = n.to + a - t.to, u = f || n.endSide - t.endSide, d = u < 0 ? n.to + a : t.to, p = Math.min(d, o);
    if (n.point || t.point ? (n.point && t.point && Cr(n.point, t.point) && zs(n.activeForPoint(n.to), t.activeForPoint(t.to)) || r.comparePoint(l, p, n.point, t.point), c = !1) : (c && r.boundChange(l), p > l && !zs(n.active, t.active) && r.compareRange(l, p, n.active, t.active), h && p < o && (f || n.openEnd(d) != t.openEnd(d)) && (c = !0)), d > o)
      break;
    l = d, u <= 0 && n.next(), u >= 0 && t.next();
  }
}
function zs(n, e) {
  if (n.length != e.length)
    return !1;
  for (let t = 0; t < n.length; t++)
    if (n[t] != e[t] && !Cr(n[t], e[t]))
      return !1;
  return !0;
}
function Xi(n, e) {
  for (let t = e, i = n.length - 1; t < i; t++)
    n[t] = n[t + 1];
  n.pop();
}
function Zi(n, e, t) {
  for (let i = n.length - 1; i >= e; i--)
    n[i + 1] = n[i];
  n[e] = t;
}
function wo(n, e) {
  let t = -1, i = 1e9;
  for (let s = 0; s < e.length; s++)
    (e[s] - i || n[s].endSide - n[t].endSide) < 0 && (t = s, i = e[s]);
  return t;
}
function Un(n, e, t = n.length) {
  let i = 0;
  for (let s = 0; s < t && s < n.length; )
    n.charCodeAt(s) == 9 ? (i += e - i % e, s++) : (i++, s = ae(n, s));
  return i;
}
function hf(n, e, t, i) {
  for (let s = 0, r = 0; ; ) {
    if (r >= e)
      return s;
    if (s == n.length)
      break;
    r += n.charCodeAt(s) == 9 ? t - r % t : 1, s = ae(n, s);
  }
  return n.length;
}
const Us = "ͼ", vo = typeof Symbol > "u" ? "__" + Us : Symbol.for(Us), js = typeof Symbol > "u" ? "__styleSet" + Math.floor(Math.random() * 1e8) : /* @__PURE__ */ Symbol("styleSet"), xo = typeof globalThis < "u" ? globalThis : typeof window < "u" ? window : {};
class dt {
  // :: (Object<Style>, ?{finish: ?(string) → string})
  // Create a style module from the given spec.
  //
  // When `finish` is given, it is called on regular (non-`@`)
  // selectors (after `&` expansion) to compute the final selector.
  constructor(e, t) {
    this.rules = [];
    let { finish: i } = t || {};
    function s(o) {
      return /^@/.test(o) ? [o] : o.split(/,\s*/);
    }
    function r(o, l, a, h) {
      let c = [], f = /^@(\w+)\b/.exec(o[0]), u = f && f[1] == "keyframes";
      if (f && l == null) return a.push(o[0] + ";");
      for (let d in l) {
        let p = l[d];
        if (/&/.test(d))
          r(
            d.split(/,\s*/).map((g) => o.map((m) => g.replace(/&/, m))).reduce((g, m) => g.concat(m)),
            p,
            a
          );
        else if (p && typeof p == "object") {
          if (!f) throw new RangeError("The value of a property (" + d + ") should be a primitive value.");
          r(s(d), p, c, u);
        } else p != null && c.push(d.replace(/_.*/, "").replace(/[A-Z]/g, (g) => "-" + g.toLowerCase()) + ": " + p + ";");
      }
      (c.length || u) && a.push((i && !f && !h ? o.map(i) : o).join(", ") + " {" + c.join(" ") + "}");
    }
    for (let o in e) r(s(o), e[o], this.rules);
  }
  // :: () → string
  // Returns a string containing the module's CSS rules.
  getRules() {
    return this.rules.join(`
`);
  }
  // :: () → string
  // Generate a new unique CSS class name.
  static newName() {
    let e = xo[vo] || 1;
    return xo[vo] = e + 1, Us + e.toString(36);
  }
  // :: (union<Document, ShadowRoot>, union<[StyleModule], StyleModule>, ?{nonce: ?string})
  //
  // Mount the given set of modules in the given DOM root, which ensures
  // that the CSS rules defined by the module are available in that
  // context.
  //
  // Rules are only added to the document once per root.
  //
  // Rule order will follow the order of the modules, so that rules from
  // modules later in the array take precedence of those from earlier
  // modules. If you call this function multiple times for the same root
  // in a way that changes the order of already mounted modules, the old
  // order will be changed.
  //
  // If a Content Security Policy nonce is provided, it is added to
  // the `<style>` tag generated by the library.
  static mount(e, t, i) {
    let s = e[js], r = i && i.nonce;
    s ? r && s.setNonce(r) : s = new cf(e, r), s.mount(Array.isArray(t) ? t : [t], e);
  }
}
let ko = /* @__PURE__ */ new Map();
class cf {
  constructor(e, t) {
    let i = e.ownerDocument || e, s = i.defaultView;
    if (!e.head && e.adoptedStyleSheets && s.CSSStyleSheet) {
      let r = ko.get(i);
      if (r) return e[js] = r;
      this.sheet = new s.CSSStyleSheet(), ko.set(i, this);
    } else
      this.styleTag = i.createElement("style"), t && this.styleTag.setAttribute("nonce", t);
    this.modules = [], e[js] = this;
  }
  mount(e, t) {
    let i = this.sheet, s = 0, r = 0;
    for (let o = 0; o < e.length; o++) {
      let l = e[o], a = this.modules.indexOf(l);
      if (a < r && a > -1 && (this.modules.splice(a, 1), r--, a = -1), a == -1) {
        if (this.modules.splice(r++, 0, l), i) for (let h = 0; h < l.rules.length; h++)
          i.insertRule(l.rules[h], s++);
      } else {
        for (; r < a; ) s += this.modules[r++].rules.length;
        s += l.rules.length, r++;
      }
    }
    if (i)
      t.adoptedStyleSheets.indexOf(this.sheet) < 0 && (t.adoptedStyleSheets = [this.sheet, ...t.adoptedStyleSheets]);
    else {
      let o = "";
      for (let a = 0; a < this.modules.length; a++)
        o += this.modules[a].getRules() + `
`;
      this.styleTag.textContent = o;
      let l = t.head || t;
      this.styleTag.parentNode != l && l.insertBefore(this.styleTag, l.firstChild);
    }
  }
  setNonce(e) {
    this.styleTag && this.styleTag.getAttribute("nonce") != e && this.styleTag.setAttribute("nonce", e);
  }
}
var pt = {
  8: "Backspace",
  9: "Tab",
  10: "Enter",
  12: "NumLock",
  13: "Enter",
  16: "Shift",
  17: "Control",
  18: "Alt",
  20: "CapsLock",
  27: "Escape",
  32: " ",
  33: "PageUp",
  34: "PageDown",
  35: "End",
  36: "Home",
  37: "ArrowLeft",
  38: "ArrowUp",
  39: "ArrowRight",
  40: "ArrowDown",
  44: "PrintScreen",
  45: "Insert",
  46: "Delete",
  59: ";",
  61: "=",
  91: "Meta",
  92: "Meta",
  106: "*",
  107: "+",
  108: ",",
  109: "-",
  110: ".",
  111: "/",
  144: "NumLock",
  145: "ScrollLock",
  160: "Shift",
  161: "Shift",
  162: "Control",
  163: "Control",
  164: "Alt",
  165: "Alt",
  173: "-",
  186: ";",
  187: "=",
  188: ",",
  189: "-",
  190: ".",
  191: "/",
  192: "`",
  219: "[",
  220: "\\",
  221: "]",
  222: "'"
}, Ei = {
  48: ")",
  49: "!",
  50: "@",
  51: "#",
  52: "$",
  53: "%",
  54: "^",
  55: "&",
  56: "*",
  57: "(",
  59: ":",
  61: "+",
  173: "_",
  186: ":",
  187: "+",
  188: "<",
  189: "_",
  190: ">",
  191: "?",
  192: "~",
  219: "{",
  220: "|",
  221: "}",
  222: '"'
}, ff = typeof navigator < "u" && /Mac/.test(navigator.platform), uf = typeof navigator < "u" && /MSIE \d|Trident\/(?:[7-9]|\d{2,})\..*rv:(\d+)/.exec(navigator.userAgent);
for (var se = 0; se < 10; se++) pt[48 + se] = pt[96 + se] = String(se);
for (var se = 1; se <= 24; se++) pt[se + 111] = "F" + se;
for (var se = 65; se <= 90; se++)
  pt[se] = String.fromCharCode(se + 32), Ei[se] = String.fromCharCode(se);
for (var cs in pt) Ei.hasOwnProperty(cs) || (Ei[cs] = pt[cs]);
function df(n) {
  var e = ff && n.metaKey && n.shiftKey && !n.ctrlKey && !n.altKey || uf && n.shiftKey && n.key && n.key.length == 1 || n.key == "Unidentified", t = !e && n.key || (n.shiftKey ? Ei : pt)[n.keyCode] || n.key || "Unidentified";
  return t == "Esc" && (t = "Escape"), t == "Del" && (t = "Delete"), t == "Left" && (t = "ArrowLeft"), t == "Up" && (t = "ArrowUp"), t == "Right" && (t = "ArrowRight"), t == "Down" && (t = "ArrowDown"), t;
}
function Ke() {
  var n = arguments[0];
  typeof n == "string" && (n = document.createElement(n));
  var e = 1, t = arguments[1];
  if (t && typeof t == "object" && t.nodeType == null && !Array.isArray(t)) {
    for (var i in t) if (Object.prototype.hasOwnProperty.call(t, i)) {
      var s = t[i];
      typeof s == "string" ? n.setAttribute(i, s) : s != null && (n[i] = s);
    }
    e++;
  }
  for (; e < arguments.length; e++) ha(n, arguments[e]);
  return n;
}
function ha(n, e) {
  if (typeof e == "string")
    n.appendChild(document.createTextNode(e));
  else if (e != null) if (e.nodeType != null)
    n.appendChild(e);
  else if (Array.isArray(e))
    for (var t = 0; t < e.length; t++) ha(n, e[t]);
  else
    throw new RangeError("Unsupported child node: " + e);
}
let ue = typeof navigator < "u" ? navigator : { userAgent: "", vendor: "", platform: "" }, qs = typeof document < "u" ? document : { documentElement: { style: {} } };
const Ks = /* @__PURE__ */ /Edge\/(\d+)/.exec(ue.userAgent), ca = /* @__PURE__ */ /MSIE \d/.test(ue.userAgent), Gs = /* @__PURE__ */ /Trident\/(?:[7-9]|\d{2,})\..*rv:(\d+)/.exec(ue.userAgent), jn = !!(ca || Gs || Ks), So = !jn && /* @__PURE__ */ /gecko\/(\d+)/i.test(ue.userAgent), fs = !jn && /* @__PURE__ */ /Chrome\/(\d+)/.exec(ue.userAgent), Ao = "webkitFontSmoothing" in qs.documentElement.style, Js = !jn && /* @__PURE__ */ /Apple Computer/.test(ue.vendor), Co = Js && (/* @__PURE__ */ /Mobile\/\w+/.test(ue.userAgent) || ue.maxTouchPoints > 2);
var C = {
  mac: Co || /* @__PURE__ */ /Mac/.test(ue.platform),
  windows: /* @__PURE__ */ /Win/.test(ue.platform),
  linux: /* @__PURE__ */ /Linux|X11/.test(ue.platform),
  ie: jn,
  ie_version: ca ? qs.documentMode || 6 : Gs ? +Gs[1] : Ks ? +Ks[1] : 0,
  gecko: So,
  gecko_version: So ? +(/* @__PURE__ */ /Firefox\/(\d+)/.exec(ue.userAgent) || [0, 0])[1] : 0,
  chrome: !!fs,
  chrome_version: fs ? +fs[1] : 0,
  ios: Co,
  android: /* @__PURE__ */ /Android\b/.test(ue.userAgent),
  webkit: Ao,
  webkit_version: Ao ? +(/* @__PURE__ */ /\bAppleWebKit\/(\d+)/.exec(ue.userAgent) || [0, 0])[1] : 0,
  safari: Js,
  safari_version: Js ? +(/* @__PURE__ */ /\bVersion\/(\d+(\.\d+)?)/.exec(ue.userAgent) || [0, 0])[1] : 0,
  tabSize: qs.documentElement.style.tabSize != null ? "tab-size" : "-moz-tab-size"
};
function Tr(n, e) {
  for (let t in n)
    t == "class" && e.class ? e.class += " " + n.class : t == "style" && e.style ? e.style += ";" + n.style : e[t] = n[t];
  return e;
}
const On = /* @__PURE__ */ Object.create(null);
function Or(n, e, t) {
  if (n == e)
    return !0;
  n || (n = On), e || (e = On);
  let i = Object.keys(n), s = Object.keys(e);
  if (i.length - 0 != s.length - 0)
    return !1;
  for (let r of i)
    if (r != t && (s.indexOf(r) == -1 || n[r] !== e[r]))
      return !1;
  return !0;
}
function pf(n, e) {
  for (let t = n.attributes.length - 1; t >= 0; t--) {
    let i = n.attributes[t].name;
    e[i] == null && n.removeAttribute(i);
  }
  for (let t in e) {
    let i = e[t];
    t == "style" ? n.style.cssText = i : n.getAttribute(t) != i && n.setAttribute(t, i);
  }
}
function Mo(n, e, t) {
  let i = !1;
  if (e)
    for (let s in e)
      t && s in t || (i = !0, s == "style" ? n.style.cssText = "" : n.removeAttribute(s));
  if (t)
    for (let s in t)
      e && e[s] == t[s] || (i = !0, s == "style" ? n.style.cssText = t[s] : n.setAttribute(s, t[s]));
  return i;
}
function gf(n) {
  let e = /* @__PURE__ */ Object.create(null);
  for (let t = 0; t < n.attributes.length; t++) {
    let i = n.attributes[t];
    e[i.name] = i.value;
  }
  return e;
}
class zi {
  /**
  Compare this instance to another instance of the same type.
  (TypeScript can't express this, but only instances of the same
  specific class will be passed to this method.) This is used to
  avoid redrawing widgets when they are replaced by a new
  decoration of the same type. The default implementation just
  returns `false`, which will cause new instances of the widget to
  always be redrawn.
  */
  eq(e) {
    return !1;
  }
  /**
  Update a DOM element created by a widget of the same type (but
  different, non-`eq` content) to reflect this widget. May return
  true to indicate that it could update, false to indicate it
  couldn't (in which case the widget will be redrawn). The default
  implementation just returns false.
  */
  updateDOM(e, t, i) {
    return !1;
  }
  /**
  @internal
  */
  compare(e) {
    return this == e || this.constructor == e.constructor && this.eq(e);
  }
  /**
  The estimated height this widget will have, to be used when
  estimating the height of content that hasn't been drawn. May
  return -1 to indicate you don't know. The default implementation
  returns -1.
  */
  get estimatedHeight() {
    return -1;
  }
  /**
  For inline widgets that are displayed inline (as opposed to
  `inline-block`) and introduce line breaks (through `<br>` tags
  or textual newlines), this must indicate the amount of line
  breaks they introduce. Defaults to 0.
  */
  get lineBreaks() {
    return 0;
  }
  /**
  Can be used to configure which kinds of events inside the widget
  should be ignored by the editor. The default is to ignore all
  events.
  */
  ignoreEvent(e) {
    return !0;
  }
  /**
  Override the way screen coordinates for positions at/in the
  widget are found. `pos` will be the offset into the widget, and
  `side` the side of the position that is being queried—less than
  zero for before, greater than zero for after, and zero for
  directly at that position.
  */
  coordsAt(e, t, i) {
    return null;
  }
  /**
  @internal
  */
  get isHidden() {
    return !1;
  }
  /**
  @internal
  */
  get editable() {
    return !1;
  }
  /**
  This is called when the an instance of the widget is removed
  from the editor view.
  */
  destroy(e) {
  }
}
var re = /* @__PURE__ */ (function(n) {
  return n[n.Text = 0] = "Text", n[n.WidgetBefore = 1] = "WidgetBefore", n[n.WidgetAfter = 2] = "WidgetAfter", n[n.WidgetRange = 3] = "WidgetRange", n;
})(re || (re = {}));
class K extends Rt {
  constructor(e, t, i, s) {
    super(), this.startSide = e, this.endSide = t, this.widget = i, this.spec = s;
  }
  /**
  @internal
  */
  get heightRelevant() {
    return !1;
  }
  /**
  Create a mark decoration, which influences the styling of the
  content in its range. Nested mark decorations will cause nested
  DOM elements to be created. Nesting order is determined by
  precedence of the [facet](https://codemirror.net/6/docs/ref/#view.EditorView^decorations), with
  the higher-precedence decorations creating the inner DOM nodes.
  Such elements are split on line boundaries and on the boundaries
  of lower-precedence decorations.
  */
  static mark(e) {
    return new Ui(e);
  }
  /**
  Create a widget decoration, which displays a DOM element at the
  given position.
  */
  static widget(e) {
    let t = Math.max(-1e4, Math.min(1e4, e.side || 0)), i = !!e.block;
    return t += i && !e.inlineOrder ? t > 0 ? 3e8 : -4e8 : t > 0 ? 1e8 : -1e8, new Bt(e, t, t, i, e.widget || null, !1);
  }
  /**
  Create a replace decoration which replaces the given range with
  a widget, or simply hides it.
  */
  static replace(e) {
    let t = !!e.block, i, s;
    if (e.isBlockGap)
      i = -5e8, s = 4e8;
    else {
      let { start: r, end: o } = fa(e, t);
      i = (r ? t ? -3e8 : -1 : 5e8) - 1, s = (o ? t ? 2e8 : 1 : -6e8) + 1;
    }
    return new Bt(e, i, s, t, e.widget || null, !0);
  }
  /**
  Create a line decoration, which can add DOM attributes to the
  line starting at the given position.
  */
  static line(e) {
    return new ji(e);
  }
  /**
  Build a [`DecorationSet`](https://codemirror.net/6/docs/ref/#view.DecorationSet) from the given
  decorated range or ranges. If the ranges aren't already sorted,
  pass `true` for `sort` to make the library sort them for you.
  */
  static set(e, t = !1) {
    return B.of(e, t);
  }
  /**
  @internal
  */
  hasHeight() {
    return this.widget ? this.widget.estimatedHeight > -1 : !1;
  }
}
K.none = B.empty;
class Ui extends K {
  constructor(e) {
    let { start: t, end: i } = fa(e);
    super(t ? -1 : 5e8, i ? 1 : -6e8, null, e), this.tagName = e.tagName || "span", this.attrs = e.class && e.attributes ? Tr(e.attributes, { class: e.class }) : e.class ? { class: e.class } : e.attributes || On;
  }
  eq(e) {
    return this == e || e instanceof Ui && this.tagName == e.tagName && Or(this.attrs, e.attrs);
  }
  range(e, t = e) {
    if (e >= t)
      throw new RangeError("Mark decorations may not be empty");
    return super.range(e, t);
  }
}
Ui.prototype.point = !1;
class ji extends K {
  constructor(e) {
    super(-2e8, -2e8, null, e);
  }
  eq(e) {
    return e instanceof ji && this.spec.class == e.spec.class && Or(this.spec.attributes, e.spec.attributes);
  }
  range(e, t = e) {
    if (t != e)
      throw new RangeError("Line decoration ranges must be zero-length");
    return super.range(e, t);
  }
}
ji.prototype.mapMode = pe.TrackBefore;
ji.prototype.point = !0;
class Bt extends K {
  constructor(e, t, i, s, r, o) {
    super(t, i, r, e), this.block = s, this.isReplace = o, this.mapMode = s ? t <= 0 ? pe.TrackBefore : pe.TrackAfter : pe.TrackDel;
  }
  // Only relevant when this.block == true
  get type() {
    return this.startSide != this.endSide ? re.WidgetRange : this.startSide <= 0 ? re.WidgetBefore : re.WidgetAfter;
  }
  get heightRelevant() {
    return this.block || !!this.widget && (this.widget.estimatedHeight >= 5 || this.widget.lineBreaks > 0);
  }
  eq(e) {
    return e instanceof Bt && mf(this.widget, e.widget) && this.block == e.block && this.startSide == e.startSide && this.endSide == e.endSide;
  }
  range(e, t = e) {
    if (this.isReplace && (e > t || e == t && this.startSide > 0 && this.endSide <= 0))
      throw new RangeError("Invalid range for replacement decoration");
    if (!this.isReplace && t != e)
      throw new RangeError("Widget decorations can only have zero-length ranges");
    return super.range(e, t);
  }
}
Bt.prototype.point = !0;
function fa(n, e = !1) {
  let { inclusiveStart: t, inclusiveEnd: i } = n;
  return t == null && (t = n.inclusive), i == null && (i = n.inclusive), { start: t ?? e, end: i ?? e };
}
function mf(n, e) {
  return n == e || !!(n && e && n.compare(e));
}
function qt(n, e, t, i = 0) {
  let s = t.length - 1;
  s >= 0 && t[s] + i >= n ? t[s] = Math.max(t[s], e) : t.push(n, e);
}
class Li extends Rt {
  constructor(e, t) {
    super(), this.tagName = e, this.attributes = t;
  }
  eq(e) {
    return e == this || e instanceof Li && this.tagName == e.tagName && Or(this.attributes, e.attributes);
  }
  /**
  Create a block wrapper object with the given tag name and
  attributes.
  */
  static create(e) {
    return new Li(e.tagName, e.attributes || On);
  }
  /**
  Create a range set from the given block wrapper ranges.
  */
  static set(e, t = !1) {
    return B.of(e, t);
  }
}
Li.prototype.startSide = Li.prototype.endSide = -1;
function Ri(n) {
  let e;
  return n.nodeType == 11 ? e = n.getSelection ? n : n.ownerDocument : e = n, e.getSelection();
}
function Ys(n, e) {
  return e ? n == e || n.contains(e.nodeType != 1 ? e.parentNode : e) : !1;
}
function xi(n, e) {
  if (!e.anchorNode)
    return !1;
  try {
    return Ys(n, e.anchorNode);
  } catch {
    return !1;
  }
}
function wn(n) {
  return n.nodeType == 3 ? Bi(n, 0, n.nodeValue.length).getClientRects() : n.nodeType == 1 ? n.getClientRects() : [];
}
function ki(n, e, t, i) {
  return t ? To(n, e, t, i, -1) || To(n, e, t, i, 1) : !1;
}
function gt(n) {
  for (var e = 0; ; e++)
    if (n = n.previousSibling, !n)
      return e;
}
function Dn(n) {
  return n.nodeType == 1 && /^(DIV|P|LI|UL|OL|BLOCKQUOTE|DD|DT|H\d|SECTION|PRE)$/.test(n.nodeName);
}
function To(n, e, t, i, s) {
  for (; ; ) {
    if (n == t && e == i)
      return !0;
    if (e == (s < 0 ? 0 : nt(n))) {
      if (n.nodeName == "DIV")
        return !1;
      let r = n.parentNode;
      if (!r || r.nodeType != 1)
        return !1;
      e = gt(n) + (s < 0 ? 0 : 1), n = r;
    } else if (n.nodeType == 1) {
      if (n = n.childNodes[e + (s < 0 ? -1 : 0)], n.nodeType == 1 && n.contentEditable == "false")
        return !1;
      e = s < 0 ? nt(n) : 0;
    } else
      return !1;
  }
}
function nt(n) {
  return n.nodeType == 3 ? n.nodeValue.length : n.childNodes.length;
}
function En(n, e) {
  let t = e ? n.left : n.right;
  return { left: t, right: t, top: n.top, bottom: n.bottom };
}
function bf(n) {
  let e = n.visualViewport;
  return e ? {
    left: 0,
    right: e.width,
    top: 0,
    bottom: e.height
  } : {
    left: 0,
    right: n.innerWidth,
    top: 0,
    bottom: n.innerHeight
  };
}
function ua(n, e) {
  let t = e.width / n.offsetWidth, i = e.height / n.offsetHeight;
  return (t > 0.995 && t < 1.005 || !isFinite(t) || Math.abs(e.width - n.offsetWidth) < 1) && (t = 1), (i > 0.995 && i < 1.005 || !isFinite(i) || Math.abs(e.height - n.offsetHeight) < 1) && (i = 1), { scaleX: t, scaleY: i };
}
function yf(n, e, t, i, s, r, o, l) {
  let a = n.ownerDocument, h = a.defaultView || window;
  for (let c = n, f = !1; c && !f; )
    if (c.nodeType == 1) {
      let u, d = c == a.body, p = 1, g = 1;
      if (d)
        u = bf(h);
      else {
        if (/^(fixed|sticky)$/.test(getComputedStyle(c).position) && (f = !0), c.scrollHeight <= c.clientHeight && c.scrollWidth <= c.clientWidth) {
          c = c.assignedSlot || c.parentNode;
          continue;
        }
        let v = c.getBoundingClientRect();
        ({ scaleX: p, scaleY: g } = ua(c, v)), u = {
          left: v.left,
          right: v.left + c.clientWidth * p,
          top: v.top,
          bottom: v.top + c.clientHeight * g
        };
      }
      let m = 0, b = 0;
      if (s == "nearest")
        e.top < u.top + o ? (b = e.top - (u.top + o), t > 0 && e.bottom > u.bottom + b && (b = e.bottom - u.bottom + o)) : e.bottom > u.bottom - o && (b = e.bottom - u.bottom + o, t < 0 && e.top - b < u.top && (b = e.top - (u.top + o)));
      else {
        let v = e.bottom - e.top, w = u.bottom - u.top;
        b = (s == "center" && v <= w ? e.top + v / 2 - w / 2 : s == "start" || s == "center" && t < 0 ? e.top - o : e.bottom - w + o) - u.top;
      }
      if (i == "nearest" ? e.left < u.left + r ? (m = e.left - (u.left + r), t > 0 && e.right > u.right + m && (m = e.right - u.right + r)) : e.right > u.right - r && (m = e.right - u.right + r, t < 0 && e.left < u.left + m && (m = e.left - (u.left + r))) : m = (i == "center" ? e.left + (e.right - e.left) / 2 - (u.right - u.left) / 2 : i == "start" == l ? e.left - r : e.right - (u.right - u.left) + r) - u.left, m || b)
        if (d)
          h.scrollBy(m, b);
        else {
          let v = 0, w = 0;
          if (b) {
            let O = c.scrollTop;
            c.scrollTop += b / g, w = (c.scrollTop - O) * g;
          }
          if (m) {
            let O = c.scrollLeft;
            c.scrollLeft += m / p, v = (c.scrollLeft - O) * p;
          }
          e = {
            left: e.left - v,
            top: e.top - w,
            right: e.right - v,
            bottom: e.bottom - w
          }, v && Math.abs(v - m) < 1 && (i = "nearest"), w && Math.abs(w - b) < 1 && (s = "nearest");
        }
      if (d)
        break;
      (e.top < u.top || e.bottom > u.bottom || e.left < u.left || e.right > u.right) && (e = {
        left: Math.max(e.left, u.left),
        right: Math.min(e.right, u.right),
        top: Math.max(e.top, u.top),
        bottom: Math.min(e.bottom, u.bottom)
      }), c = c.assignedSlot || c.parentNode;
    } else if (c.nodeType == 11)
      c = c.host;
    else
      break;
}
function da(n, e = !0) {
  let t = n.ownerDocument, i = null, s = null;
  for (let r = n.parentNode; r && !(r == t.body || (!e || i) && s); )
    if (r.nodeType == 1)
      !s && r.scrollHeight > r.clientHeight && (s = r), e && !i && r.scrollWidth > r.clientWidth && (i = r), r = r.assignedSlot || r.parentNode;
    else if (r.nodeType == 11)
      r = r.host;
    else
      break;
  return { x: i, y: s };
}
class wf {
  constructor() {
    this.anchorNode = null, this.anchorOffset = 0, this.focusNode = null, this.focusOffset = 0;
  }
  eq(e) {
    return this.anchorNode == e.anchorNode && this.anchorOffset == e.anchorOffset && this.focusNode == e.focusNode && this.focusOffset == e.focusOffset;
  }
  setRange(e) {
    let { anchorNode: t, focusNode: i } = e;
    this.set(t, Math.min(e.anchorOffset, t ? nt(t) : 0), i, Math.min(e.focusOffset, i ? nt(i) : 0));
  }
  set(e, t, i, s) {
    this.anchorNode = e, this.anchorOffset = t, this.focusNode = i, this.focusOffset = s;
  }
}
let St = null;
C.safari && C.safari_version >= 26 && (St = !1);
function pa(n) {
  if (n.setActive)
    return n.setActive();
  if (St)
    return n.focus(St);
  let e = [];
  for (let t = n; t && (e.push(t, t.scrollTop, t.scrollLeft), t != t.ownerDocument); t = t.parentNode)
    ;
  if (n.focus(St == null ? {
    get preventScroll() {
      return St = { preventScroll: !0 }, !0;
    }
  } : void 0), !St) {
    St = !1;
    for (let t = 0; t < e.length; ) {
      let i = e[t++], s = e[t++], r = e[t++];
      i.scrollTop != s && (i.scrollTop = s), i.scrollLeft != r && (i.scrollLeft = r);
    }
  }
}
let Oo;
function Bi(n, e, t = e) {
  let i = Oo || (Oo = document.createRange());
  return i.setEnd(n, t), i.setStart(n, e), i;
}
function Kt(n, e, t, i) {
  let s = { key: e, code: e, keyCode: t, which: t, cancelable: !0 };
  i && ({ altKey: s.altKey, ctrlKey: s.ctrlKey, shiftKey: s.shiftKey, metaKey: s.metaKey } = i);
  let r = new KeyboardEvent("keydown", s);
  r.synthetic = !0, n.dispatchEvent(r);
  let o = new KeyboardEvent("keyup", s);
  return o.synthetic = !0, n.dispatchEvent(o), r.defaultPrevented || o.defaultPrevented;
}
function vf(n) {
  for (; n; ) {
    if (n && (n.nodeType == 9 || n.nodeType == 11 && n.host))
      return n;
    n = n.assignedSlot || n.parentNode;
  }
  return null;
}
function xf(n, e) {
  let t = e.focusNode, i = e.focusOffset;
  if (!t || e.anchorNode != t || e.anchorOffset != i)
    return !1;
  for (i = Math.min(i, nt(t)); ; )
    if (i) {
      if (t.nodeType != 1)
        return !1;
      let s = t.childNodes[i - 1];
      s.contentEditable == "false" ? i-- : (t = s, i = nt(t));
    } else {
      if (t == n)
        return !0;
      i = gt(t), t = t.parentNode;
    }
}
function ga(n) {
  return n instanceof Window ? n.pageYOffset > Math.max(0, n.document.documentElement.scrollHeight - n.innerHeight - 4) : n.scrollTop > Math.max(1, n.scrollHeight - n.clientHeight - 4);
}
function ma(n, e) {
  for (let t = n, i = e; ; ) {
    if (t.nodeType == 3 && i > 0)
      return { node: t, offset: i };
    if (t.nodeType == 1 && i > 0) {
      if (t.contentEditable == "false")
        return null;
      t = t.childNodes[i - 1], i = nt(t);
    } else if (t.parentNode && !Dn(t))
      i = gt(t), t = t.parentNode;
    else
      return null;
  }
}
function ba(n, e) {
  for (let t = n, i = e; ; ) {
    if (t.nodeType == 3 && i < t.nodeValue.length)
      return { node: t, offset: i };
    if (t.nodeType == 1 && i < t.childNodes.length) {
      if (t.contentEditable == "false")
        return null;
      t = t.childNodes[i], i = 0;
    } else if (t.parentNode && !Dn(t))
      i = gt(t) + 1, t = t.parentNode;
    else
      return null;
  }
}
class Be {
  constructor(e, t, i = !0) {
    this.node = e, this.offset = t, this.precise = i;
  }
  static before(e, t) {
    return new Be(e.parentNode, gt(e), t);
  }
  static after(e, t) {
    return new Be(e.parentNode, gt(e) + 1, t);
  }
}
var q = /* @__PURE__ */ (function(n) {
  return n[n.LTR = 0] = "LTR", n[n.RTL = 1] = "RTL", n;
})(q || (q = {}));
const Pt = q.LTR, Dr = q.RTL;
function ya(n) {
  let e = [];
  for (let t = 0; t < n.length; t++)
    e.push(1 << +n[t]);
  return e;
}
const kf = /* @__PURE__ */ ya("88888888888888888888888888888888888666888888787833333333337888888000000000000000000000000008888880000000000000000000000000088888888888888888888888888888888888887866668888088888663380888308888800000000000000000000000800000000000000000000000000000008"), Sf = /* @__PURE__ */ ya("4444448826627288999999999992222222222222222222222222222222222222222222222229999999999999999999994444444444644222822222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222999999949999999229989999223333333333"), Xs = /* @__PURE__ */ Object.create(null), _e = [];
for (let n of ["()", "[]", "{}"]) {
  let e = /* @__PURE__ */ n.charCodeAt(0), t = /* @__PURE__ */ n.charCodeAt(1);
  Xs[e] = t, Xs[t] = -e;
}
function wa(n) {
  return n <= 247 ? kf[n] : 1424 <= n && n <= 1524 ? 2 : 1536 <= n && n <= 1785 ? Sf[n - 1536] : 1774 <= n && n <= 2220 ? 4 : 8192 <= n && n <= 8204 ? 256 : 64336 <= n && n <= 65023 ? 4 : 1;
}
const Af = /[\u0590-\u05f4\u0600-\u06ff\u0700-\u08ac\ufb50-\ufdff]/;
class Je {
  /**
  The direction of this span.
  */
  get dir() {
    return this.level % 2 ? Dr : Pt;
  }
  /**
  @internal
  */
  constructor(e, t, i) {
    this.from = e, this.to = t, this.level = i;
  }
  /**
  @internal
  */
  side(e, t) {
    return this.dir == t == e ? this.to : this.from;
  }
  /**
  @internal
  */
  forward(e, t) {
    return e == (this.dir == t);
  }
  /**
  @internal
  */
  static find(e, t, i, s) {
    let r = -1;
    for (let o = 0; o < e.length; o++) {
      let l = e[o];
      if (l.from <= t && l.to >= t) {
        if (l.level == i)
          return o;
        (r < 0 || (s != 0 ? s < 0 ? l.from < t : l.to > t : e[r].level > l.level)) && (r = o);
      }
    }
    if (r < 0)
      throw new RangeError("Index out of range");
    return r;
  }
}
function va(n, e) {
  if (n.length != e.length)
    return !1;
  for (let t = 0; t < n.length; t++) {
    let i = n[t], s = e[t];
    if (i.from != s.from || i.to != s.to || i.direction != s.direction || !va(i.inner, s.inner))
      return !1;
  }
  return !0;
}
const z = [];
function Cf(n, e, t, i, s) {
  for (let r = 0; r <= i.length; r++) {
    let o = r ? i[r - 1].to : e, l = r < i.length ? i[r].from : t, a = r ? 256 : s;
    for (let h = o, c = a, f = a; h < l; h++) {
      let u = wa(n.charCodeAt(h));
      u == 512 ? u = c : u == 8 && f == 4 && (u = 16), z[h] = u == 4 ? 2 : u, u & 7 && (f = u), c = u;
    }
    for (let h = o, c = a, f = a; h < l; h++) {
      let u = z[h];
      if (u == 128)
        h < l - 1 && c == z[h + 1] && c & 24 ? u = z[h] = c : z[h] = 256;
      else if (u == 64) {
        let d = h + 1;
        for (; d < l && z[d] == 64; )
          d++;
        let p = h && c == 8 || d < t && z[d] == 8 ? f == 1 ? 1 : 8 : 256;
        for (let g = h; g < d; g++)
          z[g] = p;
        h = d - 1;
      } else u == 8 && f == 1 && (z[h] = 1);
      c = u, u & 7 && (f = u);
    }
  }
}
function Mf(n, e, t, i, s) {
  let r = s == 1 ? 2 : 1;
  for (let o = 0, l = 0, a = 0; o <= i.length; o++) {
    let h = o ? i[o - 1].to : e, c = o < i.length ? i[o].from : t;
    for (let f = h, u, d, p; f < c; f++)
      if (d = Xs[u = n.charCodeAt(f)])
        if (d < 0) {
          for (let g = l - 3; g >= 0; g -= 3)
            if (_e[g + 1] == -d) {
              let m = _e[g + 2], b = m & 2 ? s : m & 4 ? m & 1 ? r : s : 0;
              b && (z[f] = z[_e[g]] = b), l = g;
              break;
            }
        } else {
          if (_e.length == 189)
            break;
          _e[l++] = f, _e[l++] = u, _e[l++] = a;
        }
      else if ((p = z[f]) == 2 || p == 1) {
        let g = p == s;
        a = g ? 0 : 1;
        for (let m = l - 3; m >= 0; m -= 3) {
          let b = _e[m + 2];
          if (b & 2)
            break;
          if (g)
            _e[m + 2] |= 2;
          else {
            if (b & 4)
              break;
            _e[m + 2] |= 4;
          }
        }
      }
  }
}
function Tf(n, e, t, i) {
  for (let s = 0, r = i; s <= t.length; s++) {
    let o = s ? t[s - 1].to : n, l = s < t.length ? t[s].from : e;
    for (let a = o; a < l; ) {
      let h = z[a];
      if (h == 256) {
        let c = a + 1;
        for (; ; )
          if (c == l) {
            if (s == t.length)
              break;
            c = t[s++].to, l = s < t.length ? t[s].from : e;
          } else if (z[c] == 256)
            c++;
          else
            break;
        let f = r == 1, u = (c < e ? z[c] : i) == 1, d = f == u ? f ? 1 : 2 : i;
        for (let p = c, g = s, m = g ? t[g - 1].to : n; p > a; )
          p == m && (p = t[--g].from, m = g ? t[g - 1].to : n), z[--p] = d;
        a = c;
      } else
        r = h, a++;
    }
  }
}
function Zs(n, e, t, i, s, r, o) {
  let l = i % 2 ? 2 : 1;
  if (i % 2 == s % 2)
    for (let a = e, h = 0; a < t; ) {
      let c = !0, f = !1;
      if (h == r.length || a < r[h].from) {
        let g = z[a];
        g != l && (c = !1, f = g == 16);
      }
      let u = !c && l == 1 ? [] : null, d = c ? i : i + 1, p = a;
      e: for (; ; )
        if (h < r.length && p == r[h].from) {
          if (f)
            break e;
          let g = r[h];
          if (!c)
            for (let m = g.to, b = h + 1; ; ) {
              if (m == t)
                break e;
              if (b < r.length && r[b].from == m)
                m = r[b++].to;
              else {
                if (z[m] == l)
                  break e;
                break;
              }
            }
          if (h++, u)
            u.push(g);
          else {
            g.from > a && o.push(new Je(a, g.from, d));
            let m = g.direction == Pt != !(d % 2);
            Qs(n, m ? i + 1 : i, s, g.inner, g.from, g.to, o), a = g.to;
          }
          p = g.to;
        } else {
          if (p == t || (c ? z[p] != l : z[p] == l))
            break;
          p++;
        }
      u ? Zs(n, a, p, i + 1, s, u, o) : a < p && o.push(new Je(a, p, d)), a = p;
    }
  else
    for (let a = t, h = r.length; a > e; ) {
      let c = !0, f = !1;
      if (!h || a > r[h - 1].to) {
        let g = z[a - 1];
        g != l && (c = !1, f = g == 16);
      }
      let u = !c && l == 1 ? [] : null, d = c ? i : i + 1, p = a;
      e: for (; ; )
        if (h && p == r[h - 1].to) {
          if (f)
            break e;
          let g = r[--h];
          if (!c)
            for (let m = g.from, b = h; ; ) {
              if (m == e)
                break e;
              if (b && r[b - 1].to == m)
                m = r[--b].from;
              else {
                if (z[m - 1] == l)
                  break e;
                break;
              }
            }
          if (u)
            u.push(g);
          else {
            g.to < a && o.push(new Je(g.to, a, d));
            let m = g.direction == Pt != !(d % 2);
            Qs(n, m ? i + 1 : i, s, g.inner, g.from, g.to, o), a = g.from;
          }
          p = g.from;
        } else {
          if (p == e || (c ? z[p - 1] != l : z[p - 1] == l))
            break;
          p--;
        }
      u ? Zs(n, p, a, i + 1, s, u, o) : p < a && o.push(new Je(p, a, d)), a = p;
    }
}
function Qs(n, e, t, i, s, r, o) {
  let l = e % 2 ? 2 : 1;
  Cf(n, s, r, i, l), Mf(n, s, r, i, l), Tf(s, r, i, l), Zs(n, s, r, e, t, i, o);
}
function Of(n, e, t) {
  if (!n)
    return [new Je(0, 0, e == Dr ? 1 : 0)];
  if (e == Pt && !t.length && !Af.test(n))
    return xa(n.length);
  if (t.length)
    for (; n.length > z.length; )
      z[z.length] = 256;
  let i = [], s = e == Pt ? 0 : 1;
  return Qs(n, s, s, t, 0, n.length, i), i;
}
function xa(n) {
  return [new Je(0, n, 0)];
}
let ka = "";
function Df(n, e, t, i, s) {
  var r;
  let o = i.head - n.from, l = Je.find(e, o, (r = i.bidiLevel) !== null && r !== void 0 ? r : -1, i.assoc), a = e[l], h = a.side(s, t);
  if (o == h) {
    let u = l += s ? 1 : -1;
    if (u < 0 || u >= e.length)
      return null;
    a = e[l = u], o = a.side(!s, t), h = a.side(s, t);
  }
  let c = ae(n.text, o, a.forward(s, t));
  (c < a.from || c > a.to) && (c = h), ka = n.text.slice(Math.min(o, c), Math.max(o, c));
  let f = l == (s ? e.length - 1 : 0) ? null : e[l + (s ? 1 : -1)];
  return f && c == h && f.level + (s ? 0 : 1) < a.level ? y.cursor(f.side(!s, t) + n.from, f.forward(s, t) ? 1 : -1, f.level) : y.cursor(c + n.from, a.forward(s, t) ? -1 : 1, a.level);
}
function Ef(n, e, t) {
  for (let i = e; i < t; i++) {
    let s = wa(n.charCodeAt(i));
    if (s == 1)
      return Pt;
    if (s == 2 || s == 4)
      return Dr;
  }
  return Pt;
}
const Sa = /* @__PURE__ */ M.define(), Aa = /* @__PURE__ */ M.define(), Ca = /* @__PURE__ */ M.define(), Ma = /* @__PURE__ */ M.define(), er = /* @__PURE__ */ M.define(), Ta = /* @__PURE__ */ M.define(), Oa = /* @__PURE__ */ M.define(), Er = /* @__PURE__ */ M.define(), Lr = /* @__PURE__ */ M.define(), Da = /* @__PURE__ */ M.define({
  combine: (n) => n.some((e) => e)
}), Ea = /* @__PURE__ */ M.define({
  combine: (n) => n.some((e) => e)
}), La = /* @__PURE__ */ M.define();
class Gt {
  constructor(e, t, i, s, r, o = !1) {
    this.range = e, this.y = t, this.x = i, this.yMargin = s, this.xMargin = r, this.isSnapshot = o;
  }
  map(e) {
    return e.empty ? this : new Gt(this.range.map(e), this.y, this.x, this.yMargin, this.xMargin, this.isSnapshot);
  }
  clip(e) {
    return this.range.to <= e.doc.length ? this : new Gt(y.cursor(e.doc.length), this.y, this.x, this.yMargin, this.xMargin, this.isSnapshot);
  }
}
const Qi = /* @__PURE__ */ W.define({ map: (n, e) => n.map(e) }), Ra = /* @__PURE__ */ W.define();
function Pe(n, e, t) {
  let i = n.facet(Ma);
  i.length ? i[0](e) : window.onerror && window.onerror(String(e), t, void 0, void 0, e) || (t ? console.error(t + ":", e) : console.error(e));
}
const et = /* @__PURE__ */ M.define({ combine: (n) => n.length ? n[0] : !0 });
let Lf = 0;
const Wt = /* @__PURE__ */ M.define({
  combine(n) {
    return n.filter((e, t) => {
      for (let i = 0; i < t; i++)
        if (n[i].plugin == e.plugin)
          return !1;
      return !0;
    });
  }
});
class be {
  constructor(e, t, i, s, r) {
    this.id = e, this.create = t, this.domEventHandlers = i, this.domEventObservers = s, this.baseExtensions = r(this), this.extension = this.baseExtensions.concat(Wt.of({ plugin: this, arg: void 0 }));
  }
  /**
  Create an extension for this plugin with the given argument.
  */
  of(e) {
    return this.baseExtensions.concat(Wt.of({ plugin: this, arg: e }));
  }
  /**
  Define a plugin from a constructor function that creates the
  plugin's value, given an editor view.
  */
  static define(e, t) {
    const { eventHandlers: i, eventObservers: s, provide: r, decorations: o } = t || {};
    return new be(Lf++, e, i, s, (l) => {
      let a = [];
      return o && a.push(qn.of((h) => {
        let c = h.plugin(l);
        return c ? o(c) : K.none;
      })), r && a.push(r(l)), a;
    });
  }
  /**
  Create a plugin for a class whose constructor takes a single
  editor view as argument.
  */
  static fromClass(e, t) {
    return be.define((i, s) => new e(i, s), t);
  }
}
class us {
  constructor(e) {
    this.spec = e, this.mustUpdate = null, this.value = null;
  }
  get plugin() {
    return this.spec && this.spec.plugin;
  }
  update(e) {
    if (this.value) {
      if (this.mustUpdate) {
        let t = this.mustUpdate;
        if (this.mustUpdate = null, this.value.update)
          try {
            this.value.update(t);
          } catch (i) {
            if (Pe(t.state, i, "CodeMirror plugin crashed"), this.value.destroy)
              try {
                this.value.destroy();
              } catch {
              }
            this.deactivate();
          }
      }
    } else if (this.spec)
      try {
        this.value = this.spec.plugin.create(e, this.spec.arg);
      } catch (t) {
        Pe(e.state, t, "CodeMirror plugin crashed"), this.deactivate();
      }
    return this;
  }
  destroy(e) {
    var t;
    if (!((t = this.value) === null || t === void 0) && t.destroy)
      try {
        this.value.destroy();
      } catch (i) {
        Pe(e.state, i, "CodeMirror plugin crashed");
      }
  }
  deactivate() {
    this.spec = this.value = null;
  }
}
const Ba = /* @__PURE__ */ M.define(), Rr = /* @__PURE__ */ M.define(), qn = /* @__PURE__ */ M.define(), Pa = /* @__PURE__ */ M.define(), Br = /* @__PURE__ */ M.define(), qi = /* @__PURE__ */ M.define(), Ia = /* @__PURE__ */ M.define();
function Do(n, e) {
  let t = n.state.facet(Ia);
  if (!t.length)
    return t;
  let i = t.map((r) => r instanceof Function ? r(n) : r), s = [];
  return B.spans(i, e.from, e.to, {
    point() {
    },
    span(r, o, l, a) {
      let h = r - e.from, c = o - e.from, f = s;
      for (let u = l.length - 1; u >= 0; u--, a--) {
        let d = l[u].spec.bidiIsolate, p;
        if (d == null && (d = Ef(e.text, h, c)), a > 0 && f.length && (p = f[f.length - 1]).to == h && p.direction == d)
          p.to = c, f = p.inner;
        else {
          let g = { from: h, to: c, direction: d, inner: [] };
          f.push(g), f = g.inner;
        }
      }
    }
  }), s;
}
const $a = /* @__PURE__ */ M.define();
function Pr(n) {
  let e = 0, t = 0, i = 0, s = 0;
  for (let r of n.state.facet($a)) {
    let o = r(n);
    o && (o.left != null && (e = Math.max(e, o.left)), o.right != null && (t = Math.max(t, o.right)), o.top != null && (i = Math.max(i, o.top)), o.bottom != null && (s = Math.max(s, o.bottom)));
  }
  return { left: e, right: t, top: i, bottom: s };
}
const mi = /* @__PURE__ */ M.define();
class Oe {
  constructor(e, t, i, s) {
    this.fromA = e, this.toA = t, this.fromB = i, this.toB = s;
  }
  join(e) {
    return new Oe(Math.min(this.fromA, e.fromA), Math.max(this.toA, e.toA), Math.min(this.fromB, e.fromB), Math.max(this.toB, e.toB));
  }
  addToSet(e) {
    let t = e.length, i = this;
    for (; t > 0; t--) {
      let s = e[t - 1];
      if (!(s.fromA > i.toA)) {
        if (s.toA < i.fromA)
          break;
        i = i.join(s), e.splice(t - 1, 1);
      }
    }
    return e.splice(t, 0, i), e;
  }
  // Extend a set to cover all the content in `ranges`, which is a
  // flat array with each pair of numbers representing fromB/toB
  // positions. These pairs are generated in unchanged ranges, so the
  // offset between doc A and doc B is the same for their start and
  // end points.
  static extendWithRanges(e, t) {
    if (t.length == 0)
      return e;
    let i = [];
    for (let s = 0, r = 0, o = 0; ; ) {
      let l = s < e.length ? e[s].fromB : 1e9, a = r < t.length ? t[r] : 1e9, h = Math.min(l, a);
      if (h == 1e9)
        break;
      let c = h + o, f = h, u = c;
      for (; ; )
        if (r < t.length && t[r] <= f) {
          let d = t[r + 1];
          r += 2, f = Math.max(f, d);
          for (let p = s; p < e.length && e[p].fromB <= f; p++)
            o = e[p].toA - e[p].toB;
          u = Math.max(u, d + o);
        } else if (s < e.length && e[s].fromB <= f) {
          let d = e[s++];
          f = Math.max(f, d.toB), u = Math.max(u, d.toA), o = d.toA - d.toB;
        } else
          break;
      i.push(new Oe(c, u, h, f));
    }
    return i;
  }
}
class Ln {
  constructor(e, t, i) {
    this.view = e, this.state = t, this.transactions = i, this.flags = 0, this.startState = e.state, this.changes = ee.empty(this.startState.doc.length);
    for (let r of i)
      this.changes = this.changes.compose(r.changes);
    let s = [];
    this.changes.iterChangedRanges((r, o, l, a) => s.push(new Oe(r, o, l, a))), this.changedRanges = s;
  }
  /**
  @internal
  */
  static create(e, t, i) {
    return new Ln(e, t, i);
  }
  /**
  Tells you whether the [viewport](https://codemirror.net/6/docs/ref/#view.EditorView.viewport) or
  [visible ranges](https://codemirror.net/6/docs/ref/#view.EditorView.visibleRanges) changed in this
  update.
  */
  get viewportChanged() {
    return (this.flags & 4) > 0;
  }
  /**
  Returns true when
  [`viewportChanged`](https://codemirror.net/6/docs/ref/#view.ViewUpdate.viewportChanged) is true
  and the viewport change is not just the result of mapping it in
  response to document changes.
  */
  get viewportMoved() {
    return (this.flags & 8) > 0;
  }
  /**
  Indicates whether the height of a block element in the editor
  changed in this update.
  */
  get heightChanged() {
    return (this.flags & 2) > 0;
  }
  /**
  Returns true when the document was modified or the size of the
  editor, or elements within the editor, changed.
  */
  get geometryChanged() {
    return this.docChanged || (this.flags & 18) > 0;
  }
  /**
  True when this update indicates a focus change.
  */
  get focusChanged() {
    return (this.flags & 1) > 0;
  }
  /**
  Whether the document changed in this update.
  */
  get docChanged() {
    return !this.changes.empty;
  }
  /**
  Whether the selection was explicitly set in this update.
  */
  get selectionSet() {
    return this.transactions.some((e) => e.selection);
  }
  /**
  @internal
  */
  get empty() {
    return this.flags == 0 && this.transactions.length == 0;
  }
}
const Rf = [];
class J {
  constructor(e, t, i = 0) {
    this.dom = e, this.length = t, this.flags = i, this.parent = null, e.cmTile = this;
  }
  get breakAfter() {
    return this.flags & 1;
  }
  get children() {
    return Rf;
  }
  isWidget() {
    return !1;
  }
  get isHidden() {
    return !1;
  }
  isComposite() {
    return !1;
  }
  isLine() {
    return !1;
  }
  isText() {
    return !1;
  }
  isBlock() {
    return !1;
  }
  get domAttrs() {
    return null;
  }
  sync(e) {
    if (this.flags |= 2, this.flags & 4) {
      this.flags &= -5;
      let t = this.domAttrs;
      t && pf(this.dom, t);
    }
  }
  toString() {
    return this.constructor.name + (this.children.length ? `(${this.children})` : "") + (this.breakAfter ? "#" : "");
  }
  destroy() {
    this.parent = null;
  }
  setDOM(e) {
    this.dom = e, e.cmTile = this;
  }
  get posAtStart() {
    return this.parent ? this.parent.posBefore(this) : 0;
  }
  get posAtEnd() {
    return this.posAtStart + this.length;
  }
  posBefore(e, t = this.posAtStart) {
    let i = t;
    for (let s of this.children) {
      if (s == e)
        return i;
      i += s.length + s.breakAfter;
    }
    throw new RangeError("Invalid child in posBefore");
  }
  posAfter(e) {
    return this.posBefore(e) + e.length;
  }
  covers(e) {
    return !0;
  }
  coordsIn(e, t) {
    return null;
  }
  domPosFor(e, t) {
    let i = gt(this.dom), s = this.length ? e > 0 : t > 0;
    return new Be(this.parent.dom, i + (s ? 1 : 0), e == 0 || e == this.length);
  }
  markDirty(e) {
    this.flags &= -3, e && (this.flags |= 4), this.parent && this.parent.flags & 2 && this.parent.markDirty(!1);
  }
  get overrideDOMText() {
    return null;
  }
  get root() {
    for (let e = this; e; e = e.parent)
      if (e instanceof Gn)
        return e;
    return null;
  }
  static get(e) {
    return e.cmTile;
  }
}
class Kn extends J {
  constructor(e) {
    super(e, 0), this._children = [];
  }
  isComposite() {
    return !0;
  }
  get children() {
    return this._children;
  }
  get lastChild() {
    return this.children.length ? this.children[this.children.length - 1] : null;
  }
  append(e) {
    this.children.push(e), e.parent = this;
  }
  sync(e) {
    if (this.flags & 2)
      return;
    super.sync(e);
    let t = this.dom, i = null, s, r = e?.node == t ? e : null, o = 0;
    for (let l of this.children) {
      if (l.sync(e), o += l.length + l.breakAfter, s = i ? i.nextSibling : t.firstChild, r && s != l.dom && (r.written = !0), l.dom.parentNode == t)
        for (; s && s != l.dom; )
          s = Eo(s);
      else
        t.insertBefore(l.dom, s);
      i = l.dom;
    }
    for (s = i ? i.nextSibling : t.firstChild, r && s && (r.written = !0); s; )
      s = Eo(s);
    this.length = o;
  }
}
function Eo(n) {
  let e = n.nextSibling;
  return n.parentNode.removeChild(n), e;
}
class Gn extends Kn {
  constructor(e, t) {
    super(t), this.view = e;
  }
  owns(e) {
    for (; e; e = e.parent)
      if (e == this)
        return !0;
    return !1;
  }
  isBlock() {
    return !0;
  }
  nearest(e) {
    for (; ; ) {
      if (!e)
        return null;
      let t = J.get(e);
      if (t && this.owns(t))
        return t;
      e = e.parentNode;
    }
  }
  blockTiles(e) {
    for (let t = [], i = this, s = 0, r = 0; ; )
      if (s == i.children.length) {
        if (!t.length)
          return;
        i = i.parent, i.breakAfter && r++, s = t.pop();
      } else {
        let o = i.children[s++];
        if (o instanceof it)
          t.push(s), i = o, s = 0;
        else {
          let l = r + o.length, a = e(o, r);
          if (a !== void 0)
            return a;
          r = l + o.breakAfter;
        }
      }
  }
  // Find the block at the given position. If side < -1, make sure to
  // stay before block widgets at that position, if side > 1, after
  // such widgets (used for selection drawing, which needs to be able
  // to get coordinates for positions that aren't valid cursor positions).
  resolveBlock(e, t) {
    let i, s = -1, r, o = -1;
    if (this.blockTiles((l, a) => {
      let h = a + l.length;
      if (e >= a && e <= h) {
        if (l.isWidget() && t >= -1 && t <= 1) {
          if (l.flags & 32)
            return !0;
          l.flags & 16 && (i = void 0);
        }
        (a < e || e == h && (t < -1 ? l.length : l.covers(1))) && (!i || !l.isWidget() && i.isWidget()) && (i = l, s = e - a), (h > e || e == a && (t > 1 ? l.length : l.covers(-1))) && (!r || !l.isWidget() && r.isWidget()) && (r = l, o = e - a);
      }
    }), !i && !r)
      throw new Error("No tile at position " + e);
    return i && t < 0 || !r ? { tile: i, offset: s } : { tile: r, offset: o };
  }
}
class it extends Kn {
  constructor(e, t) {
    super(e), this.wrapper = t;
  }
  isBlock() {
    return !0;
  }
  covers(e) {
    return this.children.length ? e < 0 ? this.children[0].covers(-1) : this.lastChild.covers(1) : !1;
  }
  get domAttrs() {
    return this.wrapper.attributes;
  }
  static of(e, t) {
    let i = new it(t || document.createElement(e.tagName), e);
    return t || (i.flags |= 4), i;
  }
}
class Zt extends Kn {
  constructor(e, t) {
    super(e), this.attrs = t;
  }
  isLine() {
    return !0;
  }
  static start(e, t, i) {
    let s = new Zt(t || document.createElement("div"), e);
    return (!t || !i) && (s.flags |= 4), s;
  }
  get domAttrs() {
    return this.attrs;
  }
  // Find the tile associated with a given position in this line.
  resolveInline(e, t, i) {
    let s = null, r = -1, o = null, l = -1;
    function a(c, f) {
      for (let u = 0, d = 0; u < c.children.length && d <= f; u++) {
        let p = c.children[u], g = d + p.length;
        g >= f && (p.isComposite() ? a(p, f - d) : (!o || o.isHidden && (t > 0 || i && Pf(o, p))) && (g > f || p.flags & 32) ? (o = p, l = f - d) : (d < f || p.flags & 16 && !p.isHidden) && (s = p, r = f - d)), d = g;
      }
    }
    a(this, e);
    let h = (t < 0 ? s : o) || s || o;
    return h ? { tile: h, offset: h == s ? r : l } : null;
  }
  coordsIn(e, t) {
    let i = this.resolveInline(e, t, !0);
    return i ? i.tile.coordsIn(Math.max(0, i.offset), t) : Bf(this);
  }
  domIn(e, t) {
    let i = this.resolveInline(e, t);
    if (i) {
      let { tile: s, offset: r } = i;
      if (this.dom.contains(s.dom))
        return s.isText() ? new Be(s.dom, Math.min(s.dom.nodeValue.length, r)) : s.domPosFor(r, s.flags & 16 ? 1 : s.flags & 32 ? -1 : t);
      let o = i.tile.parent, l = !1;
      for (let a of o.children) {
        if (l)
          return new Be(a.dom, 0);
        a == i.tile && (l = !0);
      }
    }
    return new Be(this.dom, 0);
  }
}
function Bf(n) {
  let e = n.dom.lastChild;
  if (!e)
    return n.dom.getBoundingClientRect();
  let t = wn(e);
  return t[t.length - 1] || null;
}
function Pf(n, e) {
  let t = n.coordsIn(0, 1), i = e.coordsIn(0, 1);
  return t && i && i.top < t.bottom;
}
class ge extends Kn {
  constructor(e, t) {
    super(e), this.mark = t;
  }
  get domAttrs() {
    return this.mark.attrs;
  }
  static of(e, t) {
    let i = new ge(t || document.createElement(e.tagName), e);
    return t || (i.flags |= 4), i;
  }
}
class Tt extends J {
  constructor(e, t) {
    super(e, t.length), this.text = t;
  }
  sync(e) {
    this.flags & 2 || (super.sync(e), this.dom.nodeValue != this.text && (e && e.node == this.dom && (e.written = !0), this.dom.nodeValue = this.text));
  }
  isText() {
    return !0;
  }
  toString() {
    return JSON.stringify(this.text);
  }
  coordsIn(e, t) {
    let i = this.dom.nodeValue.length;
    e > i && (e = i);
    let s = e, r = e, o = 0;
    e == 0 && t < 0 || e == i && t >= 0 ? C.chrome || C.gecko || (e ? (s--, o = 1) : r < i && (r++, o = -1)) : t < 0 ? s-- : r < i && r++;
    let l = Bi(this.dom, s, r).getClientRects();
    if (!l.length)
      return null;
    let a = l[(o ? o < 0 : t >= 0) ? 0 : l.length - 1];
    return C.safari && !o && a.width == 0 && (a = Array.prototype.find.call(l, (h) => h.width) || a), o ? En(a, o < 0) : a || null;
  }
  static of(e, t) {
    let i = new Tt(t || document.createTextNode(e), e);
    return t || (i.flags |= 2), i;
  }
}
class It extends J {
  constructor(e, t, i, s) {
    super(e, t, s), this.widget = i;
  }
  isWidget() {
    return !0;
  }
  get isHidden() {
    return this.widget.isHidden;
  }
  covers(e) {
    return this.flags & 48 ? !1 : (this.flags & (e < 0 ? 64 : 128)) > 0;
  }
  coordsIn(e, t) {
    return this.coordsInWidget(e, t, !1);
  }
  coordsInWidget(e, t, i) {
    let s = this.widget.coordsAt(this.dom, e, t);
    if (s)
      return s;
    if (i)
      return En(this.dom.getBoundingClientRect(), this.length ? e == 0 : t <= 0);
    {
      let r = this.dom.getClientRects(), o = null;
      if (!r.length)
        return null;
      let l = this.flags & 16 ? !0 : this.flags & 32 ? !1 : e > 0;
      for (let a = l ? r.length - 1 : 0; o = r[a], !(e > 0 ? a == 0 : a == r.length - 1 || o.top < o.bottom); a += l ? -1 : 1)
        ;
      return En(o, !l);
    }
  }
  get overrideDOMText() {
    if (!this.length)
      return $.empty;
    let { root: e } = this;
    if (!e)
      return $.empty;
    let t = this.posAtStart;
    return e.view.state.doc.slice(t, t + this.length);
  }
  destroy() {
    super.destroy(), this.widget.destroy(this.dom);
  }
  static of(e, t, i, s, r) {
    return r || (r = e.toDOM(t), e.editable || (r.contentEditable = "false")), new It(r, i, e, s);
  }
}
class Rn extends J {
  constructor(e) {
    let t = document.createElement("img");
    t.className = "cm-widgetBuffer", t.setAttribute("aria-hidden", "true"), super(t, 0, e);
  }
  get isHidden() {
    return !0;
  }
  get overrideDOMText() {
    return $.empty;
  }
  coordsIn(e) {
    return this.dom.getBoundingClientRect();
  }
}
class If {
  constructor(e) {
    this.index = 0, this.beforeBreak = !1, this.parents = [], this.tile = e;
  }
  // Advance by the given distance. If side is -1, stop leaving or
  // entering tiles, or skipping zero-length tiles, once the distance
  // has been traversed. When side is 1, leave, enter, or skip
  // everything at the end position.
  advance(e, t, i) {
    let { tile: s, index: r, beforeBreak: o, parents: l } = this;
    for (; e || t > 0; )
      if (s.isComposite())
        if (o) {
          if (!e)
            break;
          i && i.break(), e--, o = !1;
        } else if (r == s.children.length) {
          if (!e && !l.length)
            break;
          i && i.leave(s), o = !!s.breakAfter, { tile: s, index: r } = l.pop(), r++;
        } else {
          let a = s.children[r], h = a.breakAfter;
          (t > 0 ? a.length <= e : a.length < e) && (!i || i.skip(a, 0, a.length) !== !1 || !a.isComposite) ? (o = !!h, r++, e -= a.length) : (l.push({ tile: s, index: r }), s = a, r = 0, i && a.isComposite() && i.enter(a));
        }
      else if (r == s.length)
        o = !!s.breakAfter, { tile: s, index: r } = l.pop(), r++;
      else if (e) {
        let a = Math.min(e, s.length - r);
        i && i.skip(s, r, r + a), e -= a, r += a;
      } else
        break;
    return this.tile = s, this.index = r, this.beforeBreak = o, this;
  }
  get root() {
    return this.parents.length ? this.parents[0].tile : this.tile;
  }
}
class $f {
  constructor(e, t, i, s) {
    this.from = e, this.to = t, this.wrapper = i, this.rank = s;
  }
}
class Nf {
  constructor(e, t, i) {
    this.cache = e, this.root = t, this.blockWrappers = i, this.curLine = null, this.lastBlock = null, this.afterWidget = null, this.pos = 0, this.wrappers = [], this.wrapperPos = 0;
  }
  addText(e, t, i, s) {
    var r;
    this.flushBuffer();
    let o = this.ensureMarks(t, i), l = o.lastChild;
    if (l && l.isText() && !(l.flags & 8) && l.length + e.length < 512) {
      this.cache.reused.set(
        l,
        2
        /* Reused.DOM */
      );
      let a = o.children[o.children.length - 1] = new Tt(l.dom, l.text + e);
      a.parent = o;
    } else
      o.append(s || Tt.of(e, (r = this.cache.find(Tt)) === null || r === void 0 ? void 0 : r.dom));
    this.pos += e.length, this.afterWidget = null;
  }
  addComposition(e, t) {
    let i = this.curLine;
    i.dom != t.line.dom && (i.setDOM(this.cache.reused.has(t.line) ? ds(t.line.dom) : t.line.dom), this.cache.reused.set(
      t.line,
      2
      /* Reused.DOM */
    ));
    let s = i;
    for (let l = t.marks.length - 1; l >= 0; l--) {
      let a = t.marks[l], h = s.lastChild;
      if (h instanceof ge && h.mark.eq(a.mark))
        h.dom != a.dom && h.setDOM(ds(a.dom)), s = h;
      else {
        if (this.cache.reused.get(a)) {
          let f = J.get(a.dom);
          f && f.setDOM(ds(a.dom));
        }
        let c = ge.of(a.mark, a.dom);
        s.append(c), s = c;
      }
      this.cache.reused.set(
        a,
        2
        /* Reused.DOM */
      );
    }
    let r = J.get(e.text);
    r && this.cache.reused.set(
      r,
      2
      /* Reused.DOM */
    );
    let o = new Tt(e.text, e.text.nodeValue);
    o.flags |= 8, this.pos = e.range.toB, s.append(o);
  }
  addInlineWidget(e, t, i) {
    let s = this.afterWidget && e.flags & 48 && (this.afterWidget.flags & 48) == (e.flags & 48);
    s || this.flushBuffer();
    let r = this.ensureMarks(t, i);
    !s && !(e.flags & 16) && r.append(this.getBuffer(1)), r.append(e), this.pos += e.length, this.afterWidget = e;
  }
  addMark(e, t, i) {
    this.flushBuffer(), this.ensureMarks(t, i).append(e), this.pos += e.length, this.afterWidget = null;
  }
  addBlockWidget(e) {
    this.getBlockPos().append(e), this.pos += e.length, this.lastBlock = e, this.endLine();
  }
  continueWidget(e) {
    let t = this.afterWidget || this.lastBlock;
    t.length += e, this.pos += e;
  }
  addLineStart(e, t) {
    var i;
    e || (e = Na);
    let s = Zt.start(e, t || ((i = this.cache.find(Zt)) === null || i === void 0 ? void 0 : i.dom), !!t);
    this.getBlockPos().append(this.lastBlock = this.curLine = s);
  }
  addLine(e) {
    this.getBlockPos().append(e), this.pos += e.length, this.lastBlock = e, this.endLine();
  }
  addBreak() {
    this.lastBlock.flags |= 1, this.endLine(), this.pos++;
  }
  addLineStartIfNotCovered(e) {
    this.blockPosCovered() || this.addLineStart(e);
  }
  ensureLine(e) {
    this.curLine || this.addLineStart(e);
  }
  ensureMarks(e, t) {
    var i;
    let s = this.curLine;
    for (let r = e.length - 1; r >= 0; r--) {
      let o = e[r], l;
      if (t > 0 && (l = s.lastChild) && l instanceof ge && l.mark.eq(o))
        s = l, t--;
      else {
        let a = ge.of(o, (i = this.cache.find(ge, (h) => h.mark.eq(o))) === null || i === void 0 ? void 0 : i.dom);
        s.append(a), s = a, t = 0;
      }
    }
    return s;
  }
  endLine() {
    if (this.curLine) {
      this.flushBuffer();
      let e = this.curLine.lastChild;
      (!e || !Lo(this.curLine, !1) || e.dom.nodeName != "BR" && e.isWidget() && !(C.ios && Lo(this.curLine, !0))) && this.curLine.append(this.cache.findWidget(
        ps,
        0,
        32
        /* TileFlag.After */
      ) || new It(
        ps.toDOM(),
        0,
        ps,
        32
        /* TileFlag.After */
      )), this.curLine = this.afterWidget = null;
    }
  }
  updateBlockWrappers() {
    this.wrapperPos > this.pos + 1e4 && (this.blockWrappers.goto(this.pos), this.wrappers.length = 0);
    for (let e = this.wrappers.length - 1; e >= 0; e--)
      this.wrappers[e].to < this.pos && this.wrappers.splice(e, 1);
    for (let e = this.blockWrappers; e.value && e.from <= this.pos; e.next())
      if (e.to >= this.pos) {
        let t = new $f(e.from, e.to, e.value, e.rank), i = this.wrappers.length;
        for (; i > 0 && (this.wrappers[i - 1].rank - t.rank || this.wrappers[i - 1].to - t.to) < 0; )
          i--;
        this.wrappers.splice(i, 0, t);
      }
    this.wrapperPos = this.pos;
  }
  getBlockPos() {
    var e;
    this.updateBlockWrappers();
    let t = this.root;
    for (let i of this.wrappers) {
      let s = t.lastChild;
      if (i.from < this.pos && s instanceof it && s.wrapper.eq(i.wrapper))
        t = s;
      else {
        let r = it.of(i.wrapper, (e = this.cache.find(it, (o) => o.wrapper.eq(i.wrapper))) === null || e === void 0 ? void 0 : e.dom);
        t.append(r), t = r;
      }
    }
    return t;
  }
  blockPosCovered() {
    let e = this.lastBlock;
    return e != null && !e.breakAfter && (!e.isWidget() || (e.flags & 160) > 0);
  }
  getBuffer(e) {
    let t = 2 | (e < 0 ? 16 : 32), i = this.cache.find(
      Rn,
      void 0,
      1
      /* Reused.Full */
    );
    return i && (i.flags = t), i || new Rn(t);
  }
  flushBuffer() {
    this.afterWidget && !(this.afterWidget.flags & 32) && (this.afterWidget.parent.append(this.getBuffer(-1)), this.afterWidget = null);
  }
}
class Hf {
  constructor(e) {
    this.skipCount = 0, this.text = "", this.textOff = 0, this.cursor = e.iter();
  }
  skip(e) {
    this.textOff + e <= this.text.length ? this.textOff += e : (this.skipCount += e - (this.text.length - this.textOff), this.text = "", this.textOff = 0);
  }
  next(e) {
    if (this.textOff == this.text.length) {
      let { value: s, lineBreak: r, done: o } = this.cursor.next(this.skipCount);
      if (this.skipCount = 0, o)
        throw new Error("Ran out of text content when drawing inline views");
      this.text = s;
      let l = this.textOff = Math.min(e, s.length);
      return r ? null : s.slice(0, l);
    }
    let t = Math.min(this.text.length, this.textOff + e), i = this.text.slice(this.textOff, t);
    return this.textOff = t, i;
  }
}
const Bn = [It, Zt, Tt, ge, Rn, it, Gn];
for (let n = 0; n < Bn.length; n++)
  Bn[n].bucket = n;
class Vf {
  constructor(e) {
    this.view = e, this.buckets = Bn.map(() => []), this.index = Bn.map(() => 0), this.reused = /* @__PURE__ */ new Map();
  }
  // Put a tile in the cache.
  add(e) {
    let t = e.constructor.bucket, i = this.buckets[t];
    i.length < 6 ? i.push(e) : i[
      this.index[t] = (this.index[t] + 1) % 6
      /* C.Bucket */
    ] = e;
  }
  find(e, t, i = 2) {
    let s = e.bucket, r = this.buckets[s], o = this.index[s];
    for (let l = r.length - 1; l >= 0; l--) {
      let a = (l + o) % r.length, h = r[a];
      if ((!t || t(h)) && !this.reused.has(h))
        return r.splice(a, 1), a < o && this.index[s]--, this.reused.set(h, i), h;
    }
    return null;
  }
  findWidget(e, t, i) {
    let s = this.buckets[0];
    if (s.length)
      for (let r = 0, o = 0; ; r++) {
        if (r == s.length) {
          if (o)
            return null;
          o = 1, r = 0;
        }
        let l = s[r];
        if (!this.reused.has(l) && (o == 0 ? l.widget.compare(e) : l.widget.constructor == e.constructor && e.updateDOM(l.dom, this.view, l.widget)))
          return s.splice(r, 1), r < this.index[0] && this.index[0]--, l.widget == e && l.length == t && (l.flags & 497) == i ? (this.reused.set(
            l,
            1
            /* Reused.Full */
          ), l) : (this.reused.set(
            l,
            2
            /* Reused.DOM */
          ), new It(l.dom, t, e, l.flags & -498 | i));
      }
  }
  reuse(e) {
    return this.reused.set(
      e,
      1
      /* Reused.Full */
    ), e;
  }
  maybeReuse(e, t = 2) {
    if (!this.reused.has(e))
      return this.reused.set(e, t), e.dom;
  }
  clear() {
    for (let e = 0; e < this.buckets.length; e++)
      this.buckets[e].length = this.index[e] = 0;
  }
}
class Ff {
  constructor(e, t, i, s, r) {
    this.view = e, this.decorations = s, this.disallowBlockEffectsFor = r, this.openWidget = !1, this.openMarks = 0, this.cache = new Vf(e), this.text = new Hf(e.state.doc), this.builder = new Nf(this.cache, new Gn(e, e.contentDOM), B.iter(i)), this.cache.reused.set(
      t,
      2
      /* Reused.DOM */
    ), this.old = new If(t), this.reuseWalker = {
      skip: (o, l, a) => {
        if (this.cache.add(o), o.isComposite())
          return !1;
      },
      enter: (o) => this.cache.add(o),
      leave: () => {
      },
      break: () => {
      }
    };
  }
  run(e, t) {
    let i = t && this.getCompositionContext(t.text);
    for (let s = 0, r = 0, o = 0; ; ) {
      let l = o < e.length ? e[o++] : null, a = l ? l.fromA : this.old.root.length;
      if (a > s) {
        let h = a - s;
        this.preserve(h, !o, !l), s = a, r += h;
      }
      if (!l)
        break;
      t && l.fromA <= t.range.fromA && l.toA >= t.range.toA ? (this.forward(l.fromA, t.range.fromA, t.range.fromA < t.range.toA ? 1 : -1), this.emit(r, t.range.fromB), this.cache.clear(), this.builder.addComposition(t, i), this.text.skip(t.range.toB - t.range.fromB), this.forward(t.range.fromA, l.toA), this.emit(t.range.toB, l.toB)) : (this.forward(l.fromA, l.toA), this.emit(r, l.toB)), r = l.toB, s = l.toA;
    }
    return this.builder.curLine && this.builder.endLine(), this.builder.root;
  }
  preserve(e, t, i) {
    let s = zf(this.old), r = this.openMarks;
    this.old.advance(e, i ? 1 : -1, {
      skip: (o, l, a) => {
        if (o.isWidget())
          if (this.openWidget)
            this.builder.continueWidget(a - l);
          else {
            let h = a > 0 || l < o.length ? It.of(o.widget, this.view, a - l, o.flags & 496, this.cache.maybeReuse(o)) : this.cache.reuse(o);
            h.flags & 256 ? (h.flags &= -2, this.builder.addBlockWidget(h)) : (this.builder.ensureLine(null), this.builder.addInlineWidget(h, s, r), r = s.length);
          }
        else if (o.isText())
          this.builder.ensureLine(null), !l && a == o.length && !this.cache.reused.has(o) ? this.builder.addText(o.text, s, r, this.cache.reuse(o)) : (this.cache.add(o), this.builder.addText(o.text.slice(l, a), s, r)), r = s.length;
        else if (o.isLine())
          o.flags &= -2, this.cache.reused.set(
            o,
            1
            /* Reused.Full */
          ), this.builder.addLine(o);
        else if (o instanceof Rn)
          this.cache.add(o);
        else if (o instanceof ge)
          this.builder.ensureLine(null), this.builder.addMark(o, s, r), this.cache.reused.set(
            o,
            1
            /* Reused.Full */
          ), r = s.length;
        else
          return !1;
        this.openWidget = !1;
      },
      enter: (o) => {
        o.isLine() ? this.builder.addLineStart(o.attrs, this.cache.maybeReuse(o)) : (this.cache.add(o), o instanceof ge && s.unshift(o.mark)), this.openWidget = !1;
      },
      leave: (o) => {
        o.isLine() ? s.length && (s.length = r = 0) : o instanceof ge && (s.shift(), r = Math.min(r, s.length));
      },
      break: () => {
        this.builder.addBreak(), this.openWidget = !1;
      }
    }), this.text.skip(e);
  }
  emit(e, t) {
    let i = null, s = this.builder, r = 0, o = B.spans(this.decorations, e, t, {
      point: (l, a, h, c, f, u) => {
        if (h instanceof Bt) {
          if (this.disallowBlockEffectsFor[u]) {
            if (h.block)
              throw new RangeError("Block decorations may not be specified via plugins");
            if (a > this.view.state.doc.lineAt(l).to)
              throw new RangeError("Decorations that replace line breaks may not be specified via plugins");
          }
          if (r = c.length, f > c.length)
            s.continueWidget(a - l);
          else {
            let d = h.widget || (h.block ? Qt.block : Qt.inline), p = Wf(h), g = this.cache.findWidget(d, a - l, p) || It.of(d, this.view, a - l, p);
            h.block ? (h.startSide > 0 && s.addLineStartIfNotCovered(i), s.addBlockWidget(g)) : (s.ensureLine(i), s.addInlineWidget(g, c, f));
          }
          i = null;
        } else
          i = _f(i, h);
        a > l && this.text.skip(a - l);
      },
      span: (l, a, h, c) => {
        for (let f = l; f < a; ) {
          let u = this.text.next(Math.min(512, a - f));
          u == null ? (s.addLineStartIfNotCovered(i), s.addBreak(), f++) : (s.ensureLine(i), s.addText(u, h, f == l ? c : h.length), f += u.length), i = null;
        }
      }
    });
    s.addLineStartIfNotCovered(i), this.openWidget = o > r, this.openMarks = o;
  }
  forward(e, t, i = 1) {
    t - e <= 10 ? this.old.advance(t - e, i, this.reuseWalker) : (this.old.advance(5, -1, this.reuseWalker), this.old.advance(t - e - 10, -1), this.old.advance(5, i, this.reuseWalker));
  }
  getCompositionContext(e) {
    let t = [], i = null;
    for (let s = e.parentNode; ; s = s.parentNode) {
      let r = J.get(s);
      if (s == this.view.contentDOM)
        break;
      r instanceof ge ? t.push(r) : r?.isLine() ? i = r : r instanceof it || (s.nodeName == "DIV" && !i && s != this.view.contentDOM ? i = new Zt(s, Na) : i || t.push(ge.of(new Ui({ tagName: s.nodeName.toLowerCase(), attributes: gf(s) }), s)));
    }
    return { line: i, marks: t };
  }
}
function Lo(n, e) {
  let t = (i) => {
    for (let s of i.children)
      if ((e ? s.isText() : s.length) || t(s))
        return !0;
    return !1;
  };
  return t(n);
}
function Wf(n) {
  let e = n.isReplace ? (n.startSide < 0 ? 64 : 0) | (n.endSide > 0 ? 128 : 0) : n.startSide > 0 ? 32 : 16;
  return n.block && (e |= 256), e;
}
const Na = { class: "cm-line" };
function _f(n, e) {
  let t = e.spec.attributes, i = e.spec.class;
  return !t && !i || (n || (n = { class: "cm-line" }), t && Tr(t, n), i && (n.class += " " + i)), n;
}
function zf(n) {
  let e = [];
  for (let t = n.parents.length; t > 1; t--) {
    let i = t == n.parents.length ? n.tile : n.parents[t].tile;
    i instanceof ge && e.push(i.mark);
  }
  return e;
}
function ds(n) {
  let e = J.get(n);
  return e && e.setDOM(n.cloneNode()), n;
}
class Qt extends zi {
  constructor(e) {
    super(), this.tag = e;
  }
  eq(e) {
    return e.tag == this.tag;
  }
  toDOM() {
    return document.createElement(this.tag);
  }
  updateDOM(e) {
    return e.nodeName.toLowerCase() == this.tag;
  }
  get isHidden() {
    return !0;
  }
}
Qt.inline = /* @__PURE__ */ new Qt("span");
Qt.block = /* @__PURE__ */ new Qt("div");
const ps = /* @__PURE__ */ new class extends zi {
  toDOM() {
    return document.createElement("br");
  }
  get isHidden() {
    return !0;
  }
  get editable() {
    return !0;
  }
}();
class Ro {
  constructor(e) {
    this.view = e, this.decorations = [], this.blockWrappers = [], this.dynamicDecorationMap = [!1], this.domChanged = null, this.hasComposition = null, this.editContextFormatting = K.none, this.lastCompositionAfterCursor = !1, this.minWidth = 0, this.minWidthFrom = 0, this.minWidthTo = 0, this.impreciseAnchor = null, this.impreciseHead = null, this.forceSelection = !1, this.lastUpdate = Date.now(), this.updateDeco(), this.tile = new Gn(e, e.contentDOM), this.updateInner([new Oe(0, 0, 0, e.state.doc.length)], null);
  }
  // Update the document view to a given state.
  update(e) {
    var t;
    let i = e.changedRanges;
    this.minWidth > 0 && i.length && (i.every(({ fromA: c, toA: f }) => f < this.minWidthFrom || c > this.minWidthTo) ? (this.minWidthFrom = e.changes.mapPos(this.minWidthFrom, 1), this.minWidthTo = e.changes.mapPos(this.minWidthTo, 1)) : this.minWidth = this.minWidthFrom = this.minWidthTo = 0), this.updateEditContextFormatting(e);
    let s = -1;
    this.view.inputState.composing >= 0 && !this.view.observer.editContext && (!((t = this.domChanged) === null || t === void 0) && t.newSel ? s = this.domChanged.newSel.head : !Zf(e.changes, this.hasComposition) && !e.selectionSet && (s = e.state.selection.main.head));
    let r = s > -1 ? jf(this.view, e.changes, s) : null;
    if (this.domChanged = null, this.hasComposition) {
      let { from: c, to: f } = this.hasComposition;
      i = new Oe(c, f, e.changes.mapPos(c, -1), e.changes.mapPos(f, 1)).addToSet(i.slice());
    }
    this.hasComposition = r ? { from: r.range.fromB, to: r.range.toB } : null, (C.ie || C.chrome) && !r && e && e.state.doc.lines != e.startState.doc.lines && (this.forceSelection = !0);
    let o = this.decorations, l = this.blockWrappers;
    this.updateDeco();
    let a = Gf(o, this.decorations, e.changes);
    a.length && (i = Oe.extendWithRanges(i, a));
    let h = Yf(l, this.blockWrappers, e.changes);
    return h.length && (i = Oe.extendWithRanges(i, h)), r && !i.some((c) => c.fromA <= r.range.fromA && c.toA >= r.range.toA) && (i = r.range.addToSet(i.slice())), this.tile.flags & 2 && i.length == 0 ? !1 : (this.updateInner(i, r), e.transactions.length && (this.lastUpdate = Date.now()), !0);
  }
  // Used by update and the constructor do perform the actual DOM
  // update
  updateInner(e, t) {
    this.view.viewState.mustMeasureContent = !0;
    let { observer: i } = this.view;
    i.ignore(() => {
      if (t || e.length) {
        let o = this.tile, l = new Ff(this.view, o, this.blockWrappers, this.decorations, this.dynamicDecorationMap);
        t && J.get(t.text) && l.cache.reused.set(
          J.get(t.text),
          2
          /* Reused.DOM */
        ), this.tile = l.run(e, t), tr(o, l.cache.reused);
      }
      this.tile.dom.style.height = this.view.viewState.contentHeight / this.view.scaleY + "px", this.tile.dom.style.flexBasis = this.minWidth ? this.minWidth + "px" : "";
      let r = C.chrome || C.ios ? { node: i.selectionRange.focusNode, written: !1 } : void 0;
      this.tile.sync(r), r && (r.written || i.selectionRange.focusNode != r.node || !this.tile.dom.contains(r.node)) && (this.forceSelection = !0), this.tile.dom.style.height = "";
    });
    let s = [];
    if (this.view.viewport.from || this.view.viewport.to < this.view.state.doc.length)
      for (let r of this.tile.children)
        r.isWidget() && r.widget instanceof gs && s.push(r.dom);
    i.updateGaps(s);
  }
  updateEditContextFormatting(e) {
    this.editContextFormatting = this.editContextFormatting.map(e.changes);
    for (let t of e.transactions)
      for (let i of t.effects)
        i.is(Ra) && (this.editContextFormatting = i.value);
  }
  // Sync the DOM selection to this.state.selection
  updateSelection(e = !1, t = !1) {
    (e || !this.view.observer.selectionRange.focusNode) && this.view.observer.readSelectionRange();
    let { dom: i } = this.tile, s = this.view.root.activeElement, r = s == i, o = !r && !(this.view.state.facet(et) || i.tabIndex > -1) && xi(i, this.view.observer.selectionRange) && !(s && i.contains(s));
    if (!(r || t || o))
      return;
    let l = this.forceSelection;
    this.forceSelection = !1;
    let a = this.view.state.selection.main, h, c;
    if (a.empty ? c = h = this.inlineDOMNearPos(a.anchor, a.assoc || 1) : (c = this.inlineDOMNearPos(a.head, a.head == a.from ? 1 : -1), h = this.inlineDOMNearPos(a.anchor, a.anchor == a.from ? 1 : -1)), C.gecko && a.empty && !this.hasComposition && Uf(h)) {
      let u = document.createTextNode("");
      this.view.observer.ignore(() => h.node.insertBefore(u, h.node.childNodes[h.offset] || null)), h = c = new Be(u, 0), l = !0;
    }
    let f = this.view.observer.selectionRange;
    (l || !f.focusNode || (!ki(h.node, h.offset, f.anchorNode, f.anchorOffset) || !ki(c.node, c.offset, f.focusNode, f.focusOffset)) && !this.suppressWidgetCursorChange(f, a)) && (this.view.observer.ignore(() => {
      C.android && C.chrome && i.contains(f.focusNode) && Xf(f.focusNode, i) && (i.blur(), i.focus({ preventScroll: !0 }));
      let u = Ri(this.view.root);
      if (u) if (a.empty) {
        if (C.gecko) {
          let d = qf(h.node, h.offset);
          if (d && d != 3) {
            let p = (d == 1 ? ma : ba)(h.node, h.offset);
            p && (h = new Be(p.node, p.offset));
          }
        }
        u.collapse(h.node, h.offset), a.bidiLevel != null && u.caretBidiLevel !== void 0 && (u.caretBidiLevel = a.bidiLevel);
      } else if (u.extend) {
        u.collapse(h.node, h.offset);
        try {
          u.extend(c.node, c.offset);
        } catch {
        }
      } else {
        let d = document.createRange();
        a.anchor > a.head && ([h, c] = [c, h]), d.setEnd(c.node, c.offset), d.setStart(h.node, h.offset), u.removeAllRanges(), u.addRange(d);
      }
      o && this.view.root.activeElement == i && (i.blur(), s && s.focus());
    }), this.view.observer.setSelectionRange(h, c)), this.impreciseAnchor = h.precise ? null : new Be(f.anchorNode, f.anchorOffset), this.impreciseHead = c.precise ? null : new Be(f.focusNode, f.focusOffset);
  }
  // If a zero-length widget is inserted next to the cursor during
  // composition, avoid moving it across it and disrupting the
  // composition.
  suppressWidgetCursorChange(e, t) {
    return this.hasComposition && t.empty && ki(e.focusNode, e.focusOffset, e.anchorNode, e.anchorOffset) && this.posFromDOM(e.focusNode, e.focusOffset) == t.head;
  }
  enforceCursorAssoc() {
    if (this.hasComposition)
      return;
    let { view: e } = this, t = e.state.selection.main, i = Ri(e.root), { anchorNode: s, anchorOffset: r } = e.observer.selectionRange;
    if (!i || !t.empty || !t.assoc || !i.modify)
      return;
    let o = this.lineAt(t.head, t.assoc);
    if (!o)
      return;
    let l = o.posAtStart;
    if (t.head == l || t.head == l + o.length)
      return;
    let a = this.coordsAt(t.head, -1), h = this.coordsAt(t.head, 1);
    if (!a || !h || a.bottom > h.top)
      return;
    let c = this.domAtPos(t.head + t.assoc, t.assoc);
    i.collapse(c.node, c.offset), i.modify("move", t.assoc < 0 ? "forward" : "backward", "lineboundary"), e.observer.readSelectionRange();
    let f = e.observer.selectionRange;
    e.docView.posFromDOM(f.anchorNode, f.anchorOffset) != t.from && i.collapse(s, r);
  }
  posFromDOM(e, t) {
    let i = this.tile.nearest(e);
    if (!i)
      return this.tile.dom.compareDocumentPosition(e) & 2 ? 0 : this.view.state.doc.length;
    let s = i.posAtStart;
    if (i.isComposite()) {
      let r;
      if (e == i.dom)
        r = i.dom.childNodes[t];
      else {
        let o = nt(e) == 0 ? 0 : t == 0 ? -1 : 1;
        for (; ; ) {
          let l = e.parentNode;
          if (l == i.dom)
            break;
          o == 0 && l.firstChild != l.lastChild && (e == l.firstChild ? o = -1 : o = 1), e = l;
        }
        o < 0 ? r = e : r = e.nextSibling;
      }
      if (r == i.dom.firstChild)
        return s;
      for (; r && !J.get(r); )
        r = r.nextSibling;
      if (!r)
        return s + i.length;
      for (let o = 0, l = s; ; o++) {
        let a = i.children[o];
        if (a.dom == r)
          return l;
        l += a.length + a.breakAfter;
      }
    } else return i.isText() ? e == i.dom ? s + t : s + (t ? i.length : 0) : s;
  }
  domAtPos(e, t) {
    let { tile: i, offset: s } = this.tile.resolveBlock(e, t);
    return i.isWidget() ? i.domPosFor(e, t) : i.domIn(s, t);
  }
  inlineDOMNearPos(e, t) {
    let i, s = -1, r = !1, o, l = -1, a = !1;
    return this.tile.blockTiles((h, c) => {
      if (h.isWidget()) {
        if (h.flags & 32 && c >= e)
          return !0;
        h.flags & 16 && (r = !0);
      } else {
        let f = c + h.length;
        if (c <= e && (i = h, s = e - c, r = f < e), f >= e && !o && (o = h, l = e - c, a = c > e), c > e && o)
          return !0;
      }
    }), !i && !o ? this.domAtPos(e, t) : (r && o ? i = null : a && i && (o = null), i && t < 0 || !o ? i.domIn(s, t) : o.domIn(l, t));
  }
  coordsAt(e, t) {
    let { tile: i, offset: s } = this.tile.resolveBlock(e, t);
    return i.isWidget() ? i.widget instanceof gs ? null : i.coordsInWidget(s, t, !0) : i.coordsIn(s, t);
  }
  lineAt(e, t) {
    let { tile: i } = this.tile.resolveBlock(e, t);
    return i.isLine() ? i : null;
  }
  coordsForChar(e) {
    let { tile: t, offset: i } = this.tile.resolveBlock(e, 1);
    if (!t.isLine())
      return null;
    function s(r, o) {
      if (r.isComposite())
        for (let l of r.children) {
          if (l.length >= o) {
            let a = s(l, o);
            if (a)
              return a;
          }
          if (o -= l.length, o < 0)
            break;
        }
      else if (r.isText() && o < r.length) {
        let l = ae(r.text, o);
        if (l == o)
          return null;
        let a = Bi(r.dom, o, l).getClientRects();
        for (let h = 0; h < a.length; h++) {
          let c = a[h];
          if (h == a.length - 1 || c.top < c.bottom && c.left < c.right)
            return c;
        }
      }
      return null;
    }
    return s(t, i);
  }
  measureVisibleLineHeights(e) {
    let t = [], { from: i, to: s } = e, r = this.view.contentDOM.clientWidth, o = r > Math.max(this.view.scrollDOM.clientWidth, this.minWidth) + 1, l = -1, a = this.view.textDirection == q.LTR, h = 0, c = (f, u, d) => {
      for (let p = 0; p < f.children.length && !(u > s); p++) {
        let g = f.children[p], m = u + g.length, b = g.dom.getBoundingClientRect(), { height: v } = b;
        if (d && !p && (h += b.top - d.top), g instanceof it)
          m > i && c(g, u, b);
        else if (u >= i && (h > 0 && t.push(-h), t.push(v + h), h = 0, o)) {
          let w = g.dom.lastChild, O = w ? wn(w) : [];
          if (O.length) {
            let k = O[O.length - 1], S = a ? k.right - b.left : b.right - k.left;
            S > l && (l = S, this.minWidth = r, this.minWidthFrom = u, this.minWidthTo = m);
          }
        }
        d && p == f.children.length - 1 && (h += d.bottom - b.bottom), u = m + g.breakAfter;
      }
    };
    return c(this.tile, 0, null), t;
  }
  textDirectionAt(e) {
    let { tile: t } = this.tile.resolveBlock(e, 1);
    return getComputedStyle(t.dom).direction == "rtl" ? q.RTL : q.LTR;
  }
  measureTextSize() {
    let e = this.tile.blockTiles((o) => {
      if (o.isLine() && o.children.length && o.length <= 20) {
        let l = 0, a;
        for (let h of o.children) {
          if (!h.isText() || /[^ -~]/.test(h.text))
            return;
          let c = wn(h.dom);
          if (c.length != 1)
            return;
          l += c[0].width, a = c[0].height;
        }
        if (l)
          return {
            lineHeight: o.dom.getBoundingClientRect().height,
            charWidth: l / o.length,
            textHeight: a
          };
      }
    });
    if (e)
      return e;
    let t = document.createElement("div"), i, s, r;
    return t.className = "cm-line", t.style.width = "99999px", t.style.position = "absolute", t.textContent = "abc def ghi jkl mno pqr stu", this.view.observer.ignore(() => {
      this.tile.dom.appendChild(t);
      let o = wn(t.firstChild)[0];
      i = t.getBoundingClientRect().height, s = o && o.width ? o.width / 27 : 7, r = o && o.height ? o.height : i, t.remove();
    }), { lineHeight: i, charWidth: s, textHeight: r };
  }
  computeBlockGapDeco() {
    let e = [], t = this.view.viewState;
    for (let i = 0, s = 0; ; s++) {
      let r = s == t.viewports.length ? null : t.viewports[s], o = r ? r.from - 1 : this.view.state.doc.length;
      if (o > i) {
        let l = (t.lineBlockAt(o).bottom - t.lineBlockAt(i).top) / this.view.scaleY;
        e.push(K.replace({
          widget: new gs(l),
          block: !0,
          inclusive: !0,
          isBlockGap: !0
        }).range(i, o));
      }
      if (!r)
        break;
      i = r.to + 1;
    }
    return K.set(e);
  }
  updateDeco() {
    let e = 1, t = this.view.state.facet(qn).map((r) => (this.dynamicDecorationMap[e++] = typeof r == "function") ? r(this.view) : r), i = !1, s = this.view.state.facet(Br).map((r, o) => {
      let l = typeof r == "function";
      return l && (i = !0), l ? r(this.view) : r;
    });
    for (s.length && (this.dynamicDecorationMap[e++] = i, t.push(B.join(s))), this.decorations = [
      this.editContextFormatting,
      ...t,
      this.computeBlockGapDeco(),
      this.view.viewState.lineGapDeco
    ]; e < this.decorations.length; )
      this.dynamicDecorationMap[e++] = !1;
    this.blockWrappers = this.view.state.facet(Pa).map((r) => typeof r == "function" ? r(this.view) : r);
  }
  scrollIntoView(e) {
    var t;
    if (e.isSnapshot) {
      let c = this.view.viewState.lineBlockAt(e.range.head);
      this.view.scrollDOM.scrollTop = c.top - e.yMargin, this.view.scrollDOM.scrollLeft = e.xMargin;
      return;
    }
    for (let c of this.view.state.facet(La))
      try {
        if (c(this.view, e.range, e))
          return !0;
      } catch (f) {
        Pe(this.view.state, f, "scroll handler");
      }
    let { range: i } = e, s = this.coordsAt(i.head, (t = i.assoc) !== null && t !== void 0 ? t : i.empty ? 0 : i.head > i.anchor ? -1 : 1), r;
    if (!s)
      return;
    !i.empty && (r = this.coordsAt(i.anchor, i.anchor > i.head ? -1 : 1)) && (s = {
      left: Math.min(s.left, r.left),
      top: Math.min(s.top, r.top),
      right: Math.max(s.right, r.right),
      bottom: Math.max(s.bottom, r.bottom)
    });
    let o = Pr(this.view), l = {
      left: s.left - o.left,
      top: s.top - o.top,
      right: s.right + o.right,
      bottom: s.bottom + o.bottom
    }, { offsetWidth: a, offsetHeight: h } = this.view.scrollDOM;
    if (yf(this.view.scrollDOM, l, i.head < i.anchor ? -1 : 1, e.x, e.y, Math.max(Math.min(e.xMargin, a), -a), Math.max(Math.min(e.yMargin, h), -h), this.view.textDirection == q.LTR), window.visualViewport && window.innerHeight - window.visualViewport.height > 1 && (s.top > window.pageYOffset + window.visualViewport.offsetTop + window.visualViewport.height || s.bottom < window.pageYOffset + window.visualViewport.offsetTop)) {
      let c = this.view.docView.lineAt(i.head, 1);
      c && c.dom.scrollIntoView({ block: "nearest" });
    }
  }
  lineHasWidget(e) {
    let t = (i) => i.isWidget() || i.children.some(t);
    return t(this.tile.resolveBlock(e, 1).tile);
  }
  destroy() {
    tr(this.tile);
  }
}
function tr(n, e) {
  let t = e?.get(n);
  if (t != 1) {
    t == null && n.destroy();
    for (let i of n.children)
      tr(i, e);
  }
}
function Uf(n) {
  return n.node.nodeType == 1 && n.node.firstChild && (n.offset == 0 || n.node.childNodes[n.offset - 1].contentEditable == "false") && (n.offset == n.node.childNodes.length || n.node.childNodes[n.offset].contentEditable == "false");
}
function Ha(n, e) {
  let t = n.observer.selectionRange;
  if (!t.focusNode)
    return null;
  let i = ma(t.focusNode, t.focusOffset), s = ba(t.focusNode, t.focusOffset), r = i || s;
  if (s && i && s.node != i.node) {
    let l = J.get(s.node);
    if (!l || l.isText() && l.text != s.node.nodeValue)
      r = s;
    else if (n.docView.lastCompositionAfterCursor) {
      let a = J.get(i.node);
      !a || a.isText() && a.text != i.node.nodeValue || (r = s);
    }
  }
  if (n.docView.lastCompositionAfterCursor = r != i, !r)
    return null;
  let o = e - r.offset;
  return { from: o, to: o + r.node.nodeValue.length, node: r.node };
}
function jf(n, e, t) {
  let i = Ha(n, t);
  if (!i)
    return null;
  let { node: s, from: r, to: o } = i, l = s.nodeValue;
  if (/[\n\r]/.test(l) || n.state.doc.sliceString(i.from, i.to) != l)
    return null;
  let a = e.invertedDesc;
  return { range: new Oe(a.mapPos(r), a.mapPos(o), r, o), text: s };
}
function qf(n, e) {
  return n.nodeType != 1 ? 0 : (e && n.childNodes[e - 1].contentEditable == "false" ? 1 : 0) | (e < n.childNodes.length && n.childNodes[e].contentEditable == "false" ? 2 : 0);
}
let Kf = class {
  constructor() {
    this.changes = [];
  }
  compareRange(e, t) {
    qt(e, t, this.changes);
  }
  comparePoint(e, t) {
    qt(e, t, this.changes);
  }
  boundChange(e) {
    qt(e, e, this.changes);
  }
};
function Gf(n, e, t) {
  let i = new Kf();
  return B.compare(n, e, t, i), i.changes;
}
class Jf {
  constructor() {
    this.changes = [];
  }
  compareRange(e, t) {
    qt(e, t, this.changes);
  }
  comparePoint() {
  }
  boundChange(e) {
    qt(e, e, this.changes);
  }
}
function Yf(n, e, t) {
  let i = new Jf();
  return B.compare(n, e, t, i), i.changes;
}
function Xf(n, e) {
  for (let t = n; t && t != e; t = t.assignedSlot || t.parentNode)
    if (t.nodeType == 1 && t.contentEditable == "false")
      return !0;
  return !1;
}
function Zf(n, e) {
  let t = !1;
  return e && n.iterChangedRanges((i, s) => {
    i < e.to && s > e.from && (t = !0);
  }), t;
}
class gs extends zi {
  constructor(e) {
    super(), this.height = e;
  }
  toDOM() {
    let e = document.createElement("div");
    return e.className = "cm-gap", this.updateDOM(e), e;
  }
  eq(e) {
    return e.height == this.height;
  }
  updateDOM(e) {
    return e.style.height = this.height + "px", !0;
  }
  get editable() {
    return !0;
  }
  get estimatedHeight() {
    return this.height;
  }
  ignoreEvent() {
    return !1;
  }
}
function Qf(n, e, t = 1) {
  let i = n.charCategorizer(e), s = n.doc.lineAt(e), r = e - s.from;
  if (s.length == 0)
    return y.cursor(e);
  r == 0 ? t = 1 : r == s.length && (t = -1);
  let o = r, l = r;
  t < 0 ? o = ae(s.text, r, !1) : l = ae(s.text, r);
  let a = i(s.text.slice(o, l));
  for (; o > 0; ) {
    let h = ae(s.text, o, !1);
    if (i(s.text.slice(h, o)) != a)
      break;
    o = h;
  }
  for (; l < s.length; ) {
    let h = ae(s.text, l);
    if (i(s.text.slice(l, h)) != a)
      break;
    l = h;
  }
  return y.range(o + s.from, l + s.from);
}
function eu(n, e, t, i, s) {
  let r = Math.round((i - e.left) * n.defaultCharacterWidth);
  if (n.lineWrapping && t.height > n.defaultLineHeight * 1.5) {
    let l = n.viewState.heightOracle.textHeight, a = Math.floor((s - t.top - (n.defaultLineHeight - l) * 0.5) / l);
    r += a * n.viewState.heightOracle.lineLength;
  }
  let o = n.state.sliceDoc(t.from, t.to);
  return t.from + hf(o, r, n.state.tabSize);
}
function ir(n, e, t) {
  let i = n.lineBlockAt(e);
  if (Array.isArray(i.type)) {
    let s;
    for (let r of i.type) {
      if (r.from > e)
        break;
      if (!(r.to < e)) {
        if (r.from < e && r.to > e)
          return r;
        (!s || r.type == re.Text && (s.type != r.type || (t < 0 ? r.from < e : r.to > e))) && (s = r);
      }
    }
    return s || i;
  }
  return i;
}
function tu(n, e, t, i) {
  let s = ir(n, e.head, e.assoc || -1), r = !i || s.type != re.Text || !(n.lineWrapping || s.widgetLineBreaks) ? null : n.coordsAtPos(e.assoc < 0 && e.head > s.from ? e.head - 1 : e.head);
  if (r) {
    let o = n.dom.getBoundingClientRect(), l = n.textDirectionAt(s.from), a = n.posAtCoords({
      x: t == (l == q.LTR) ? o.right - 1 : o.left + 1,
      y: (r.top + r.bottom) / 2
    });
    if (a != null)
      return y.cursor(a, t ? -1 : 1);
  }
  return y.cursor(t ? s.to : s.from, t ? -1 : 1);
}
function Bo(n, e, t, i) {
  let s = n.state.doc.lineAt(e.head), r = n.bidiSpans(s), o = n.textDirectionAt(s.from);
  for (let l = e, a = null; ; ) {
    let h = Df(s, r, o, l, t), c = ka;
    if (!h) {
      if (s.number == (t ? n.state.doc.lines : 1))
        return l;
      c = `
`, s = n.state.doc.line(s.number + (t ? 1 : -1)), r = n.bidiSpans(s), h = n.visualLineSide(s, !t);
    }
    if (a) {
      if (!a(c))
        return l;
    } else {
      if (!i)
        return h;
      a = i(c);
    }
    l = h;
  }
}
function iu(n, e, t) {
  let i = n.state.charCategorizer(e), s = i(t);
  return (r) => {
    let o = i(r);
    return s == tt.Space && (s = o), s == o;
  };
}
function nu(n, e, t, i) {
  let s = e.head, r = t ? 1 : -1;
  if (s == (t ? n.state.doc.length : 0))
    return y.cursor(s, e.assoc);
  let o = e.goalColumn, l, a = n.contentDOM.getBoundingClientRect(), h = n.coordsAtPos(s, e.assoc || ((e.empty ? t : e.head == e.from) ? 1 : -1)), c = n.documentTop;
  if (h)
    o == null && (o = h.left - a.left), l = r < 0 ? h.top : h.bottom;
  else {
    let p = n.viewState.lineBlockAt(s);
    o == null && (o = Math.min(a.right - a.left, n.defaultCharacterWidth * (s - p.from))), l = (r < 0 ? p.top : p.bottom) + c;
  }
  let f = a.left + o, u = n.viewState.heightOracle.textHeight >> 1, d = i ?? u;
  for (let p = 0; ; p += u) {
    let g = l + (d + p) * r, m = nr(n, { x: f, y: g }, !1, r);
    if (t ? g > a.bottom : g < a.top)
      return y.cursor(m.pos, m.assoc);
    let b = n.coordsAtPos(m.pos, m.assoc), v = b ? (b.top + b.bottom) / 2 : 0;
    if (!b || (t ? v > l : v < l))
      return y.cursor(m.pos, m.assoc, void 0, o);
  }
}
function Si(n, e, t) {
  for (; ; ) {
    let i = 0;
    for (let s of n)
      s.between(e - 1, e + 1, (r, o, l) => {
        if (e > r && e < o) {
          let a = i || t || (e - r < o - e ? -1 : 1);
          e = a < 0 ? r : o, i = a;
        }
      });
    if (!i)
      return e;
  }
}
function Va(n, e) {
  let t = null;
  for (let i = 0; i < e.ranges.length; i++) {
    let s = e.ranges[i], r = null;
    if (s.empty) {
      let o = Si(n, s.from, 0);
      o != s.from && (r = y.cursor(o, -1));
    } else {
      let o = Si(n, s.from, -1), l = Si(n, s.to, 1);
      (o != s.from || l != s.to) && (r = y.range(s.from == s.anchor ? o : l, s.from == s.head ? o : l));
    }
    r && (t || (t = e.ranges.slice()), t[i] = r);
  }
  return t ? y.create(t, e.mainIndex) : e;
}
function ms(n, e, t) {
  let i = Si(n.state.facet(qi).map((s) => s(n)), t.from, e.head > t.from ? -1 : 1);
  return i == t.from ? t : y.cursor(i, i < t.from ? 1 : -1);
}
class Ge {
  constructor(e, t) {
    this.pos = e, this.assoc = t;
  }
}
function nr(n, e, t, i) {
  let s = n.contentDOM.getBoundingClientRect(), r = s.top + n.viewState.paddingTop, { x: o, y: l } = e, a = l - r, h;
  for (; ; ) {
    if (a < 0)
      return new Ge(0, 1);
    if (a > n.viewState.docHeight)
      return new Ge(n.state.doc.length, -1);
    if (h = n.elementAtHeight(a), i == null)
      break;
    if (h.type == re.Text) {
      if (i < 0 ? h.to < n.viewport.from : h.from > n.viewport.to)
        break;
      let u = n.docView.coordsAt(i < 0 ? h.from : h.to, i > 0 ? -1 : 1);
      if (u && (i < 0 ? u.top <= a + r : u.bottom >= a + r))
        break;
    }
    let f = n.viewState.heightOracle.textHeight / 2;
    a = i > 0 ? h.bottom + f : h.top - f;
  }
  if (n.viewport.from >= h.to || n.viewport.to <= h.from) {
    if (t)
      return null;
    if (h.type == re.Text) {
      let f = eu(n, s, h, o, l);
      return new Ge(f, f == h.from ? 1 : -1);
    }
  }
  if (h.type != re.Text)
    return a < (h.top + h.bottom) / 2 ? new Ge(h.from, 1) : new Ge(h.to, -1);
  let c = n.docView.lineAt(h.from, 2);
  return (!c || c.length != h.length) && (c = n.docView.lineAt(h.from, -2)), new su(n, o, l, n.textDirectionAt(h.from)).scanTile(c, h.from);
}
class su {
  constructor(e, t, i, s) {
    this.view = e, this.x = t, this.y = i, this.baseDir = s, this.line = null, this.spans = null;
  }
  bidiSpansAt(e) {
    return (!this.line || this.line.from > e || this.line.to < e) && (this.line = this.view.state.doc.lineAt(e), this.spans = this.view.bidiSpans(this.line)), this;
  }
  baseDirAt(e, t) {
    let { line: i, spans: s } = this.bidiSpansAt(e);
    return s[Je.find(s, e - i.from, -1, t)].level == this.baseDir;
  }
  dirAt(e, t) {
    let { line: i, spans: s } = this.bidiSpansAt(e);
    return s[Je.find(s, e - i.from, -1, t)].dir;
  }
  // Used to short-circuit bidi tests for content with a uniform direction
  bidiIn(e, t) {
    let { spans: i, line: s } = this.bidiSpansAt(e);
    return i.length > 1 || i.length && (i[0].level != this.baseDir || i[0].to + s.from < t);
  }
  // Scan through the rectangles for the content of a tile with inline
  // content, looking for one that overlaps the queried position
  // vertically andis
  // closest horizontally. The caller is responsible for dividing its
  // content into N pieces, and pass an array with N+1 positions
  // (including the position after the last piece). For a text tile,
  // these will be character clusters, for a composite tile, these
  // will be child tiles.
  scan(e, t, i = !1) {
    let s = 0, r = e.length - 1, o = /* @__PURE__ */ new Set(), l = this.bidiIn(e[0], e[r]), a, h, c = -1, f = 1e9, u;
    e: for (; s < r; ) {
      let p = r - s, g = s + r >> 1;
      t: if (o.has(g)) {
        let b = s + Math.floor(Math.random() * p);
        for (let v = 0; v < p; v++) {
          if (!o.has(b)) {
            g = b;
            break t;
          }
          b++, b == r && (b = s);
        }
        break e;
      }
      o.add(g);
      let m = t(g);
      if (m)
        for (let b = 0; b < m.length; b++) {
          let v = m[b], w = 0;
          if (!(v.width == 0 && m.length > 1)) {
            if (v.bottom < this.y)
              (!a || a.bottom < v.bottom) && (a = v), w = 1;
            else if (v.top > this.y)
              (!h || h.top > v.top) && (h = v), w = -1;
            else {
              let O = v.left > this.x ? this.x - v.left : v.right < this.x ? this.x - v.right : 0, k = Math.abs(O);
              k < f && (c = g, f = k, u = v), O && (w = O < 0 == (this.baseDir == q.LTR) ? -1 : 1);
            }
            w == -1 && (!l || this.baseDirAt(e[g], 1)) ? r = g : w == 1 && (!l || this.baseDirAt(e[g + 1], -1)) && (s = g + 1);
          }
        }
    }
    if (!u) {
      let p = a && (!h || this.y - a.bottom < h.top - this.y) ? a : h;
      return this.y = (p.top + p.bottom) / 2, this.scan(e, t, !0);
    }
    if (f && !i) {
      let { top: p, bottom: g } = u;
      if (a && a.bottom > (p + p + g) / 3)
        return this.y = a.bottom - 1, this.scan(e, t, !0);
      if (h && h.top < (p + g + g) / 3)
        return this.y = h.top + 1, this.scan(e, t, !0);
    }
    let d = (l ? this.dirAt(e[c], 1) : this.baseDir) == q.LTR;
    return {
      i: c,
      // Test whether x is closes to the start or end of this element
      after: this.x > (u.left + u.right) / 2 == d
    };
  }
  scanText(e, t) {
    let i = [];
    for (let r = 0; r < e.length; r = ae(e.text, r))
      i.push(t + r);
    i.push(t + e.length);
    let s = this.scan(i, (r) => {
      let o = i[r] - t, l = i[r + 1] - t;
      return Bi(e.dom, o, l).getClientRects();
    });
    return s.after ? new Ge(i[s.i + 1], -1) : new Ge(i[s.i], 1);
  }
  scanTile(e, t) {
    if (!e.length)
      return new Ge(t, 1);
    if (e.children.length == 1) {
      let l = e.children[0];
      if (l.isText())
        return this.scanText(l, t);
      if (l.isComposite())
        return this.scanTile(l, t);
    }
    let i = [t];
    for (let l = 0, a = t; l < e.children.length; l++)
      i.push(a += e.children[l].length);
    let s = this.scan(i, (l) => {
      let a = e.children[l];
      return a.flags & 48 ? null : (a.dom.nodeType == 1 ? a.dom : Bi(a.dom, 0, a.length)).getClientRects();
    }), r = e.children[s.i], o = i[s.i];
    return r.isText() ? this.scanText(r, o) : r.isComposite() ? this.scanTile(r, o) : s.after ? new Ge(i[s.i + 1], -1) : new Ge(o, 1);
  }
}
const Vt = "￿";
class ru {
  constructor(e, t) {
    this.points = e, this.view = t, this.text = "", this.lineSeparator = t.state.facet(N.lineSeparator);
  }
  append(e) {
    this.text += e;
  }
  lineBreak() {
    this.text += Vt;
  }
  readRange(e, t) {
    if (!e)
      return this;
    let i = e.parentNode;
    for (let s = e; ; ) {
      this.findPointBefore(i, s);
      let r = this.text.length;
      this.readNode(s);
      let o = J.get(s), l = s.nextSibling;
      if (l == t) {
        o?.breakAfter && !l && i != this.view.contentDOM && this.lineBreak();
        break;
      }
      let a = J.get(l);
      (o && a ? o.breakAfter : (o ? o.breakAfter : Dn(s)) || Dn(l) && (s.nodeName != "BR" || o?.isWidget()) && this.text.length > r) && !lu(l, t) && this.lineBreak(), s = l;
    }
    return this.findPointBefore(i, t), this;
  }
  readTextNode(e) {
    let t = e.nodeValue;
    for (let i of this.points)
      i.node == e && (i.pos = this.text.length + Math.min(i.offset, t.length));
    for (let i = 0, s = this.lineSeparator ? null : /\r\n?|\n/g; ; ) {
      let r = -1, o = 1, l;
      if (this.lineSeparator ? (r = t.indexOf(this.lineSeparator, i), o = this.lineSeparator.length) : (l = s.exec(t)) && (r = l.index, o = l[0].length), this.append(t.slice(i, r < 0 ? t.length : r)), r < 0)
        break;
      if (this.lineBreak(), o > 1)
        for (let a of this.points)
          a.node == e && a.pos > this.text.length && (a.pos -= o - 1);
      i = r + o;
    }
  }
  readNode(e) {
    let t = J.get(e), i = t && t.overrideDOMText;
    if (i != null) {
      this.findPointInside(e, i.length);
      for (let s = i.iter(); !s.next().done; )
        s.lineBreak ? this.lineBreak() : this.append(s.value);
    } else e.nodeType == 3 ? this.readTextNode(e) : e.nodeName == "BR" ? e.nextSibling && this.lineBreak() : e.nodeType == 1 && this.readRange(e.firstChild, null);
  }
  findPointBefore(e, t) {
    for (let i of this.points)
      i.node == e && e.childNodes[i.offset] == t && (i.pos = this.text.length);
  }
  findPointInside(e, t) {
    for (let i of this.points)
      (e.nodeType == 3 ? i.node == e : e.contains(i.node)) && (i.pos = this.text.length + (ou(e, i.node, i.offset) ? t : 0));
  }
}
function ou(n, e, t) {
  for (; ; ) {
    if (!e || t < nt(e))
      return !1;
    if (e == n)
      return !0;
    t = gt(e) + 1, e = e.parentNode;
  }
}
function lu(n, e) {
  let t;
  for (; !(n == e || !n); n = n.nextSibling) {
    let i = J.get(n);
    if (!i?.isWidget())
      return !1;
    i && (t || (t = [])).push(i);
  }
  if (t)
    for (let i of t) {
      let s = i.overrideDOMText;
      if (s?.length)
        return !1;
    }
  return !0;
}
class Po {
  constructor(e, t) {
    this.node = e, this.offset = t, this.pos = -1;
  }
}
class au {
  constructor(e, t, i, s) {
    this.typeOver = s, this.bounds = null, this.text = "", this.domChanged = t > -1;
    let { impreciseHead: r, impreciseAnchor: o } = e.docView, l = e.state.selection;
    if (e.state.readOnly && t > -1)
      this.newSel = null;
    else if (t > -1 && (this.bounds = Fa(e.docView.tile, t, i, 0))) {
      let a = r || o ? [] : cu(e), h = new ru(a, e);
      h.readRange(this.bounds.startDOM, this.bounds.endDOM), this.text = h.text, this.newSel = fu(a, this.bounds.from);
    } else {
      let a = e.observer.selectionRange, h = r && r.node == a.focusNode && r.offset == a.focusOffset || !Ys(e.contentDOM, a.focusNode) ? l.main.head : e.docView.posFromDOM(a.focusNode, a.focusOffset), c = o && o.node == a.anchorNode && o.offset == a.anchorOffset || !Ys(e.contentDOM, a.anchorNode) ? l.main.anchor : e.docView.posFromDOM(a.anchorNode, a.anchorOffset), f = e.viewport;
      if ((C.ios || C.chrome) && l.main.empty && h != c && (f.from > 0 || f.to < e.state.doc.length)) {
        let u = Math.min(h, c), d = Math.max(h, c), p = f.from - u, g = f.to - d;
        (p == 0 || p == 1 || u == 0) && (g == 0 || g == -1 || d == e.state.doc.length) && (h = 0, c = e.state.doc.length);
      }
      if (e.inputState.composing > -1 && l.ranges.length > 1)
        this.newSel = l.replaceRange(y.range(c, h));
      else if (e.lineWrapping && c == h && !(l.main.empty && l.main.head == h) && e.inputState.lastTouchTime > Date.now() - 100) {
        let u = e.coordsAtPos(h, -1), d = 0;
        u && (d = e.inputState.lastTouchY <= u.bottom ? -1 : 1), this.newSel = y.create([y.cursor(h, d)]);
      } else
        this.newSel = y.single(c, h);
    }
  }
}
function Fa(n, e, t, i) {
  if (n.isComposite()) {
    let s = -1, r = -1, o = -1, l = -1;
    for (let a = 0, h = i, c = i; a < n.children.length; a++) {
      let f = n.children[a], u = h + f.length;
      if (h < e && u > t)
        return Fa(f, e, t, h);
      if (u >= e && s == -1 && (s = a, r = h), h > t && f.dom.parentNode == n.dom) {
        o = a, l = c;
        break;
      }
      c = u, h = u + f.breakAfter;
    }
    return {
      from: r,
      to: l < 0 ? i + n.length : l,
      startDOM: (s ? n.children[s - 1].dom.nextSibling : null) || n.dom.firstChild,
      endDOM: o < n.children.length && o >= 0 ? n.children[o].dom : null
    };
  } else return n.isText() ? { from: i, to: i + n.length, startDOM: n.dom, endDOM: n.dom.nextSibling } : null;
}
function Wa(n, e) {
  let t, { newSel: i } = e, { state: s } = n, r = s.selection.main, o = n.inputState.lastKeyTime > Date.now() - 100 ? n.inputState.lastKeyCode : -1;
  if (e.bounds) {
    let { from: l, to: a } = e.bounds, h = r.from, c = null;
    (o === 8 || C.android && e.text.length < a - l) && (h = r.to, c = "end");
    let f = s.doc.sliceString(l, a, Vt), u, d;
    !r.empty && r.from >= l && r.to <= a && (e.typeOver || f != e.text) && f.slice(0, r.from - l) == e.text.slice(0, r.from - l) && f.slice(r.to - l) == e.text.slice(u = e.text.length - (f.length - (r.to - l))) ? t = {
      from: r.from,
      to: r.to,
      insert: $.of(e.text.slice(r.from - l, u).split(Vt))
    } : (d = _a(f, e.text, h - l, c)) && (C.chrome && o == 13 && d.toB == d.from + 2 && e.text.slice(d.from, d.toB) == Vt + Vt && d.toB--, t = {
      from: l + d.from,
      to: l + d.toA,
      insert: $.of(e.text.slice(d.from, d.toB).split(Vt))
    });
  } else i && (!n.hasFocus && s.facet(et) || Pn(i, r)) && (i = null);
  if (!t && !i)
    return !1;
  if ((C.mac || C.android) && t && t.from == t.to && t.from == r.head - 1 && /^\. ?$/.test(t.insert.toString()) && n.contentDOM.getAttribute("autocorrect") == "off" ? (i && t.insert.length == 2 && (i = y.single(i.main.anchor - 1, i.main.head - 1)), t = { from: t.from, to: t.to, insert: $.of([t.insert.toString().replace(".", " ")]) }) : s.doc.lineAt(r.from).to < r.to && n.docView.lineHasWidget(r.to) && n.inputState.insertingTextAt > Date.now() - 50 ? t = {
    from: r.from,
    to: r.to,
    insert: s.toText(n.inputState.insertingText)
  } : C.chrome && t && t.from == t.to && t.from == r.head && t.insert.toString() == `
 ` && n.lineWrapping && (i && (i = y.single(i.main.anchor - 1, i.main.head - 1)), t = { from: r.from, to: r.to, insert: $.of([" "]) }), t)
    return Ir(n, t, i, o);
  if (i && !Pn(i, r)) {
    let l = !1, a = "select";
    return n.inputState.lastSelectionTime > Date.now() - 50 && (n.inputState.lastSelectionOrigin == "select" && (l = !0), a = n.inputState.lastSelectionOrigin, a == "select.pointer" && (i = Va(s.facet(qi).map((h) => h(n)), i))), n.dispatch({ selection: i, scrollIntoView: l, userEvent: a }), !0;
  } else
    return !1;
}
function Ir(n, e, t, i = -1) {
  if (C.ios && n.inputState.flushIOSKey(e))
    return !0;
  let s = n.state.selection.main;
  if (C.android && (e.to == s.to && // GBoard will sometimes remove a space it just inserted
  // after a completion when you press enter
  (e.from == s.from || e.from == s.from - 1 && n.state.sliceDoc(e.from, s.from) == " ") && e.insert.length == 1 && e.insert.lines == 2 && Kt(n.contentDOM, "Enter", 13) || (e.from == s.from - 1 && e.to == s.to && e.insert.length == 0 || i == 8 && e.insert.length < e.to - e.from && e.to > s.head) && Kt(n.contentDOM, "Backspace", 8) || e.from == s.from && e.to == s.to + 1 && e.insert.length == 0 && Kt(n.contentDOM, "Delete", 46)))
    return !0;
  let r = e.insert.toString();
  n.inputState.composing >= 0 && n.inputState.composing++;
  let o, l = () => o || (o = hu(n, e, t));
  return n.state.facet(Ta).some((a) => a(n, e.from, e.to, r, l)) || n.dispatch(l()), !0;
}
function hu(n, e, t) {
  let i, s = n.state, r = s.selection.main, o = -1;
  if (e.from == e.to && e.from < r.from || e.from > r.to) {
    let a = e.from < r.from ? -1 : 1, h = a < 0 ? r.from : r.to, c = Si(s.facet(qi).map((f) => f(n)), h, a);
    e.from == c && (o = c);
  }
  if (o > -1)
    i = {
      changes: e,
      selection: y.cursor(e.from + e.insert.length, -1)
    };
  else if (e.from >= r.from && e.to <= r.to && e.to - e.from >= (r.to - r.from) / 3 && (!t || t.main.empty && t.main.from == e.from + e.insert.length) && n.inputState.composing < 0) {
    let a = r.from < e.from ? s.sliceDoc(r.from, e.from) : "", h = r.to > e.to ? s.sliceDoc(e.to, r.to) : "";
    i = s.replaceSelection(n.state.toText(a + e.insert.sliceString(0, void 0, n.state.lineBreak) + h));
  } else {
    let a = s.changes(e), h = t && t.main.to <= a.newLength ? t.main : void 0;
    if (s.selection.ranges.length > 1 && (n.inputState.composing >= 0 || n.inputState.compositionPendingChange) && e.to <= r.to + 10 && e.to >= r.to - 10) {
      let c = n.state.sliceDoc(e.from, e.to), f, u = t && Ha(n, t.main.head);
      if (u) {
        let p = e.insert.length - (e.to - e.from);
        f = { from: u.from, to: u.to - p };
      } else
        f = n.state.doc.lineAt(r.head);
      let d = r.to - e.to;
      i = s.changeByRange((p) => {
        if (p.from == r.from && p.to == r.to)
          return { changes: a, range: h || p.map(a) };
        let g = p.to - d, m = g - c.length;
        if (n.state.sliceDoc(m, g) != c || // Unfortunately, there's no way to make multiple
        // changes in the same node work without aborting
        // composition, so cursors in the composition range are
        // ignored.
        g >= f.from && m <= f.to)
          return { range: p };
        let b = s.changes({ from: m, to: g, insert: e.insert }), v = p.to - r.to;
        return {
          changes: b,
          range: h ? y.range(Math.max(0, h.anchor + v), Math.max(0, h.head + v)) : p.map(b)
        };
      });
    } else
      i = {
        changes: a,
        selection: h && s.selection.replaceRange(h)
      };
  }
  let l = "input.type";
  return (n.composing || n.inputState.compositionPendingChange && n.inputState.compositionEndedAt > Date.now() - 50) && (n.inputState.compositionPendingChange = !1, l += ".compose", n.inputState.compositionFirstChange && (l += ".start", n.inputState.compositionFirstChange = !1)), s.update(i, { userEvent: l, scrollIntoView: !0 });
}
function _a(n, e, t, i) {
  let s = Math.min(n.length, e.length), r = 0;
  for (; r < s && n.charCodeAt(r) == e.charCodeAt(r); )
    r++;
  if (r == s && n.length == e.length)
    return null;
  let o = n.length, l = e.length;
  for (; o > 0 && l > 0 && n.charCodeAt(o - 1) == e.charCodeAt(l - 1); )
    o--, l--;
  if (i == "end") {
    let a = Math.max(0, r - Math.min(o, l));
    t -= o + a - r;
  }
  if (o < r && n.length < e.length) {
    let a = t <= r && t >= o ? r - t : 0;
    r -= a, l = r + (l - o), o = r;
  } else if (l < r) {
    let a = t <= r && t >= l ? r - t : 0;
    r -= a, o = r + (o - l), l = r;
  }
  return { from: r, toA: o, toB: l };
}
function cu(n) {
  let e = [];
  if (n.root.activeElement != n.contentDOM)
    return e;
  let { anchorNode: t, anchorOffset: i, focusNode: s, focusOffset: r } = n.observer.selectionRange;
  return t && (e.push(new Po(t, i)), (s != t || r != i) && e.push(new Po(s, r))), e;
}
function fu(n, e) {
  if (n.length == 0)
    return null;
  let t = n[0].pos, i = n.length == 2 ? n[1].pos : t;
  return t > -1 && i > -1 ? y.single(t + e, i + e) : null;
}
function Pn(n, e) {
  return e.head == n.main.head && e.anchor == n.main.anchor;
}
class uu {
  setSelectionOrigin(e) {
    this.lastSelectionOrigin = e, this.lastSelectionTime = Date.now();
  }
  constructor(e) {
    this.view = e, this.lastKeyCode = 0, this.lastKeyTime = 0, this.lastTouchTime = 0, this.lastTouchX = 0, this.lastTouchY = 0, this.lastFocusTime = 0, this.lastScrollTop = 0, this.lastScrollLeft = 0, this.lastWheelEvent = 0, this.pendingIOSKey = void 0, this.tabFocusMode = -1, this.lastSelectionOrigin = null, this.lastSelectionTime = 0, this.lastContextMenu = 0, this.scrollHandlers = [], this.handlers = /* @__PURE__ */ Object.create(null), this.composing = -1, this.compositionFirstChange = null, this.compositionEndedAt = 0, this.compositionPendingKey = !1, this.compositionPendingChange = !1, this.insertingText = "", this.insertingTextAt = 0, this.mouseSelection = null, this.draggedContent = null, this.handleEvent = this.handleEvent.bind(this), this.notifiedFocused = e.hasFocus, C.safari && e.contentDOM.addEventListener("input", () => null), C.gecko && Tu(e.contentDOM.ownerDocument);
  }
  handleEvent(e) {
    !vu(this.view, e) || this.ignoreDuringComposition(e) || e.type == "keydown" && this.keydown(e) || (this.view.updateState != 0 ? Promise.resolve().then(() => this.runHandlers(e.type, e)) : this.runHandlers(e.type, e));
  }
  runHandlers(e, t) {
    let i = this.handlers[e];
    if (i) {
      for (let s of i.observers)
        s(this.view, t);
      for (let s of i.handlers) {
        if (t.defaultPrevented)
          break;
        if (s(this.view, t)) {
          t.preventDefault();
          break;
        }
      }
    }
  }
  ensureHandlers(e) {
    let t = du(e), i = this.handlers, s = this.view.contentDOM;
    for (let r in t)
      if (r != "scroll") {
        let o = !t[r].handlers.length, l = i[r];
        l && o != !l.handlers.length && (s.removeEventListener(r, this.handleEvent), l = null), l || s.addEventListener(r, this.handleEvent, { passive: o });
      }
    for (let r in i)
      r != "scroll" && !t[r] && s.removeEventListener(r, this.handleEvent);
    this.handlers = t;
  }
  keydown(e) {
    if (this.lastKeyCode = e.keyCode, this.lastKeyTime = Date.now(), e.keyCode == 9 && this.tabFocusMode > -1 && (!this.tabFocusMode || Date.now() <= this.tabFocusMode))
      return !0;
    if (this.tabFocusMode > 0 && e.keyCode != 27 && Ua.indexOf(e.keyCode) < 0 && (this.tabFocusMode = -1), C.android && C.chrome && !e.synthetic && (e.keyCode == 13 || e.keyCode == 8))
      return this.view.observer.delayAndroidKey(e.key, e.keyCode), !0;
    let t;
    return C.ios && !e.synthetic && !e.altKey && !e.metaKey && !e.shiftKey && ((t = za.find((i) => i.keyCode == e.keyCode)) && !e.ctrlKey || pu.indexOf(e.key) > -1 && e.ctrlKey) ? (this.pendingIOSKey = t || e, setTimeout(() => this.flushIOSKey(), 250), !0) : (e.keyCode != 229 && this.view.observer.forceFlush(), !1);
  }
  flushIOSKey(e) {
    let t = this.pendingIOSKey;
    return !t || t.key == "Enter" && e && e.from < e.to && /^\S+$/.test(e.insert.toString()) ? !1 : (this.pendingIOSKey = void 0, Kt(this.view.contentDOM, t.key, t.keyCode, t instanceof KeyboardEvent ? t : void 0));
  }
  ignoreDuringComposition(e) {
    return !/^key/.test(e.type) || e.synthetic ? !1 : this.composing > 0 ? !0 : C.safari && !C.ios && this.compositionPendingKey && Date.now() - this.compositionEndedAt < 100 ? (this.compositionPendingKey = !1, !0) : !1;
  }
  startMouseSelection(e) {
    this.mouseSelection && this.mouseSelection.destroy(), this.mouseSelection = e;
  }
  update(e) {
    this.view.observer.update(e), this.mouseSelection && this.mouseSelection.update(e), this.draggedContent && e.docChanged && (this.draggedContent = this.draggedContent.map(e.changes)), e.transactions.length && (this.lastKeyCode = this.lastSelectionTime = 0);
  }
  destroy() {
    this.mouseSelection && this.mouseSelection.destroy();
  }
}
function Io(n, e) {
  return (t, i) => {
    try {
      return e.call(n, i, t);
    } catch (s) {
      Pe(t.state, s);
    }
  };
}
function du(n) {
  let e = /* @__PURE__ */ Object.create(null);
  function t(i) {
    return e[i] || (e[i] = { observers: [], handlers: [] });
  }
  for (let i of n) {
    let s = i.spec, r = s && s.plugin.domEventHandlers, o = s && s.plugin.domEventObservers;
    if (r)
      for (let l in r) {
        let a = r[l];
        a && t(l).handlers.push(Io(i.value, a));
      }
    if (o)
      for (let l in o) {
        let a = o[l];
        a && t(l).observers.push(Io(i.value, a));
      }
  }
  for (let i in $e)
    t(i).handlers.push($e[i]);
  for (let i in ye)
    t(i).observers.push(ye[i]);
  return e;
}
const za = [
  { key: "Backspace", keyCode: 8, inputType: "deleteContentBackward" },
  { key: "Enter", keyCode: 13, inputType: "insertParagraph" },
  { key: "Enter", keyCode: 13, inputType: "insertLineBreak" },
  { key: "Delete", keyCode: 46, inputType: "deleteContentForward" }
], pu = "dthko", Ua = [16, 17, 18, 20, 91, 92, 224, 225], en = 6;
function tn(n) {
  return Math.max(0, n) * 0.7 + 8;
}
function gu(n, e) {
  return Math.max(Math.abs(n.clientX - e.clientX), Math.abs(n.clientY - e.clientY));
}
class mu {
  constructor(e, t, i, s) {
    this.view = e, this.startEvent = t, this.style = i, this.mustSelect = s, this.scrollSpeed = { x: 0, y: 0 }, this.scrolling = -1, this.lastEvent = t, this.scrollParents = da(e.contentDOM), this.atoms = e.state.facet(qi).map((o) => o(e));
    let r = e.contentDOM.ownerDocument;
    r.addEventListener("mousemove", this.move = this.move.bind(this)), r.addEventListener("mouseup", this.up = this.up.bind(this)), this.extend = t.shiftKey, this.multiple = e.state.facet(N.allowMultipleSelections) && bu(e, t), this.dragging = wu(e, t) && Ka(t) == 1 ? null : !1;
  }
  start(e) {
    this.dragging === !1 && this.select(e);
  }
  move(e) {
    if (e.buttons == 0)
      return this.destroy();
    if (this.dragging || this.dragging == null && gu(this.startEvent, e) < 10)
      return;
    this.select(this.lastEvent = e);
    let t = 0, i = 0, s = 0, r = 0, o = this.view.win.innerWidth, l = this.view.win.innerHeight;
    this.scrollParents.x && ({ left: s, right: o } = this.scrollParents.x.getBoundingClientRect()), this.scrollParents.y && ({ top: r, bottom: l } = this.scrollParents.y.getBoundingClientRect());
    let a = Pr(this.view);
    e.clientX - a.left <= s + en ? t = -tn(s - e.clientX) : e.clientX + a.right >= o - en && (t = tn(e.clientX - o)), e.clientY - a.top <= r + en ? i = -tn(r - e.clientY) : e.clientY + a.bottom >= l - en && (i = tn(e.clientY - l)), this.setScrollSpeed(t, i);
  }
  up(e) {
    this.dragging == null && this.select(this.lastEvent), this.dragging || e.preventDefault(), this.destroy();
  }
  destroy() {
    this.setScrollSpeed(0, 0);
    let e = this.view.contentDOM.ownerDocument;
    e.removeEventListener("mousemove", this.move), e.removeEventListener("mouseup", this.up), this.view.inputState.mouseSelection = this.view.inputState.draggedContent = null;
  }
  setScrollSpeed(e, t) {
    this.scrollSpeed = { x: e, y: t }, e || t ? this.scrolling < 0 && (this.scrolling = setInterval(() => this.scroll(), 50)) : this.scrolling > -1 && (clearInterval(this.scrolling), this.scrolling = -1);
  }
  scroll() {
    let { x: e, y: t } = this.scrollSpeed;
    e && this.scrollParents.x && (this.scrollParents.x.scrollLeft += e, e = 0), t && this.scrollParents.y && (this.scrollParents.y.scrollTop += t, t = 0), (e || t) && this.view.win.scrollBy(e, t), this.dragging === !1 && this.select(this.lastEvent);
  }
  select(e) {
    let { view: t } = this, i = Va(this.atoms, this.style.get(e, this.extend, this.multiple));
    (this.mustSelect || !i.eq(t.state.selection, this.dragging === !1)) && this.view.dispatch({
      selection: i,
      userEvent: "select.pointer"
    }), this.mustSelect = !1;
  }
  update(e) {
    e.transactions.some((t) => t.isUserEvent("input.type")) ? this.destroy() : this.style.update(e) && setTimeout(() => this.select(this.lastEvent), 20);
  }
}
function bu(n, e) {
  let t = n.state.facet(Sa);
  return t.length ? t[0](e) : C.mac ? e.metaKey : e.ctrlKey;
}
function yu(n, e) {
  let t = n.state.facet(Aa);
  return t.length ? t[0](e) : C.mac ? !e.altKey : !e.ctrlKey;
}
function wu(n, e) {
  let { main: t } = n.state.selection;
  if (t.empty)
    return !1;
  let i = Ri(n.root);
  if (!i || i.rangeCount == 0)
    return !0;
  let s = i.getRangeAt(0).getClientRects();
  for (let r = 0; r < s.length; r++) {
    let o = s[r];
    if (o.left <= e.clientX && o.right >= e.clientX && o.top <= e.clientY && o.bottom >= e.clientY)
      return !0;
  }
  return !1;
}
function vu(n, e) {
  if (!e.bubbles)
    return !0;
  if (e.defaultPrevented)
    return !1;
  for (let t = e.target, i; t != n.contentDOM; t = t.parentNode)
    if (!t || t.nodeType == 11 || (i = J.get(t)) && i.isWidget() && !i.isHidden && i.widget.ignoreEvent(e))
      return !1;
  return !0;
}
const $e = /* @__PURE__ */ Object.create(null), ye = /* @__PURE__ */ Object.create(null), ja = C.ie && C.ie_version < 15 || C.ios && C.webkit_version < 604;
function xu(n) {
  let e = n.dom.parentNode;
  if (!e)
    return;
  let t = e.appendChild(document.createElement("textarea"));
  t.style.cssText = "position: fixed; left: -10000px; top: 10px", t.focus(), setTimeout(() => {
    n.focus(), t.remove(), qa(n, t.value);
  }, 50);
}
function Jn(n, e, t) {
  for (let i of n.facet(e))
    t = i(t, n);
  return t;
}
function qa(n, e) {
  e = Jn(n.state, Er, e);
  let { state: t } = n, i, s = 1, r = t.toText(e), o = r.lines == t.selection.ranges.length;
  if (sr != null && t.selection.ranges.every((a) => a.empty) && sr == r.toString()) {
    let a = -1;
    i = t.changeByRange((h) => {
      let c = t.doc.lineAt(h.from);
      if (c.from == a)
        return { range: h };
      a = c.from;
      let f = t.toText((o ? r.line(s++).text : e) + t.lineBreak);
      return {
        changes: { from: c.from, insert: f },
        range: y.cursor(h.from + f.length)
      };
    });
  } else o ? i = t.changeByRange((a) => {
    let h = r.line(s++);
    return {
      changes: { from: a.from, to: a.to, insert: h.text },
      range: y.cursor(a.from + h.length)
    };
  }) : i = t.replaceSelection(r);
  n.dispatch(i, {
    userEvent: "input.paste",
    scrollIntoView: !0
  });
}
ye.scroll = (n) => {
  n.inputState.lastScrollTop = n.scrollDOM.scrollTop, n.inputState.lastScrollLeft = n.scrollDOM.scrollLeft;
};
ye.wheel = ye.mousewheel = (n) => {
  n.inputState.lastWheelEvent = Date.now();
};
$e.keydown = (n, e) => (n.inputState.setSelectionOrigin("select"), e.keyCode == 27 && n.inputState.tabFocusMode != 0 && (n.inputState.tabFocusMode = Date.now() + 2e3), !1);
ye.touchstart = (n, e) => {
  let t = n.inputState, i = e.targetTouches[0];
  t.lastTouchTime = Date.now(), i && (t.lastTouchX = i.clientX, t.lastTouchY = i.clientY), t.setSelectionOrigin("select.pointer");
};
ye.touchmove = (n) => {
  n.inputState.setSelectionOrigin("select.pointer");
};
$e.mousedown = (n, e) => {
  if (n.observer.flush(), n.inputState.lastTouchTime > Date.now() - 2e3)
    return !1;
  let t = null;
  for (let i of n.state.facet(Ca))
    if (t = i(n, e), t)
      break;
  if (!t && e.button == 0 && (t = Su(n, e)), t) {
    let i = !n.hasFocus;
    n.inputState.startMouseSelection(new mu(n, e, t, i)), i && n.observer.ignore(() => {
      pa(n.contentDOM);
      let r = n.root.activeElement;
      r && !r.contains(n.contentDOM) && r.blur();
    });
    let s = n.inputState.mouseSelection;
    if (s)
      return s.start(e), s.dragging === !1;
  } else
    n.inputState.setSelectionOrigin("select.pointer");
  return !1;
};
function $o(n, e, t, i) {
  if (i == 1)
    return y.cursor(e, t);
  if (i == 2)
    return Qf(n.state, e, t);
  {
    let s = n.docView.lineAt(e, t), r = n.state.doc.lineAt(s ? s.posAtEnd : e), o = s ? s.posAtStart : r.from, l = s ? s.posAtEnd : r.to;
    return l < n.state.doc.length && l == r.to && l++, y.range(o, l);
  }
}
const ku = C.ie && C.ie_version <= 11;
let No = null, Ho = 0, Vo = 0;
function Ka(n) {
  if (!ku)
    return n.detail;
  let e = No, t = Vo;
  return No = n, Vo = Date.now(), Ho = !e || t > Date.now() - 400 && Math.abs(e.clientX - n.clientX) < 2 && Math.abs(e.clientY - n.clientY) < 2 ? (Ho + 1) % 3 : 1;
}
function Su(n, e) {
  let t = n.posAndSideAtCoords({ x: e.clientX, y: e.clientY }, !1), i = Ka(e), s = n.state.selection;
  return {
    update(r) {
      r.docChanged && (t.pos = r.changes.mapPos(t.pos), s = s.map(r.changes));
    },
    get(r, o, l) {
      let a = n.posAndSideAtCoords({ x: r.clientX, y: r.clientY }, !1), h, c = $o(n, a.pos, a.assoc, i);
      if (t.pos != a.pos && !o) {
        let f = $o(n, t.pos, t.assoc, i), u = Math.min(f.from, c.from), d = Math.max(f.to, c.to);
        c = u < c.from ? y.range(u, d, c.assoc) : y.range(d, u, c.assoc);
      }
      return o ? s.replaceRange(s.main.extend(c.from, c.to, c.assoc)) : l && i == 1 && s.ranges.length > 1 && (h = Au(s, a.pos)) ? h : l ? s.addRange(c) : y.create([c]);
    }
  };
}
function Au(n, e) {
  for (let t = 0; t < n.ranges.length; t++) {
    let { from: i, to: s } = n.ranges[t];
    if (i <= e && s >= e)
      return y.create(n.ranges.slice(0, t).concat(n.ranges.slice(t + 1)), n.mainIndex == t ? 0 : n.mainIndex - (n.mainIndex > t ? 1 : 0));
  }
  return null;
}
$e.dragstart = (n, e) => {
  let { selection: { main: t } } = n.state;
  if (e.target.draggable) {
    let s = n.docView.tile.nearest(e.target);
    if (s && s.isWidget()) {
      let r = s.posAtStart, o = r + s.length;
      (r >= t.to || o <= t.from) && (t = y.range(r, o));
    }
  }
  let { inputState: i } = n;
  return i.mouseSelection && (i.mouseSelection.dragging = !0), i.draggedContent = t, e.dataTransfer && (e.dataTransfer.setData("Text", Jn(n.state, Lr, n.state.sliceDoc(t.from, t.to))), e.dataTransfer.effectAllowed = "copyMove"), !1;
};
$e.dragend = (n) => (n.inputState.draggedContent = null, !1);
function Fo(n, e, t, i) {
  if (t = Jn(n.state, Er, t), !t)
    return;
  let s = n.posAtCoords({ x: e.clientX, y: e.clientY }, !1), { draggedContent: r } = n.inputState, o = i && r && yu(n, e) ? { from: r.from, to: r.to } : null, l = { from: s, insert: t }, a = n.state.changes(o ? [o, l] : l);
  n.focus(), n.dispatch({
    changes: a,
    selection: { anchor: a.mapPos(s, -1), head: a.mapPos(s, 1) },
    userEvent: o ? "move.drop" : "input.drop"
  }), n.inputState.draggedContent = null;
}
$e.drop = (n, e) => {
  if (!e.dataTransfer)
    return !1;
  if (n.state.readOnly)
    return !0;
  let t = e.dataTransfer.files;
  if (t && t.length) {
    let i = Array(t.length), s = 0, r = () => {
      ++s == t.length && Fo(n, e, i.filter((o) => o != null).join(n.state.lineBreak), !1);
    };
    for (let o = 0; o < t.length; o++) {
      let l = new FileReader();
      l.onerror = r, l.onload = () => {
        /[\x00-\x08\x0e-\x1f]{2}/.test(l.result) || (i[o] = l.result), r();
      }, l.readAsText(t[o]);
    }
    return !0;
  } else {
    let i = e.dataTransfer.getData("Text");
    if (i)
      return Fo(n, e, i, !0), !0;
  }
  return !1;
};
$e.paste = (n, e) => {
  if (n.state.readOnly)
    return !0;
  n.observer.flush();
  let t = ja ? null : e.clipboardData;
  return t ? (qa(n, t.getData("text/plain") || t.getData("text/uri-list")), !0) : (xu(n), !1);
};
function Cu(n, e) {
  let t = n.dom.parentNode;
  if (!t)
    return;
  let i = t.appendChild(document.createElement("textarea"));
  i.style.cssText = "position: fixed; left: -10000px; top: 10px", i.value = e, i.focus(), i.selectionEnd = e.length, i.selectionStart = 0, setTimeout(() => {
    i.remove(), n.focus();
  }, 50);
}
function Mu(n) {
  let e = [], t = [], i = !1;
  for (let s of n.selection.ranges)
    s.empty || (e.push(n.sliceDoc(s.from, s.to)), t.push(s));
  if (!e.length) {
    let s = -1;
    for (let { from: r } of n.selection.ranges) {
      let o = n.doc.lineAt(r);
      o.number > s && (e.push(o.text), t.push({ from: o.from, to: Math.min(n.doc.length, o.to + 1) })), s = o.number;
    }
    i = !0;
  }
  return { text: Jn(n, Lr, e.join(n.lineBreak)), ranges: t, linewise: i };
}
let sr = null;
$e.copy = $e.cut = (n, e) => {
  if (!xi(n.contentDOM, n.observer.selectionRange))
    return !1;
  let { text: t, ranges: i, linewise: s } = Mu(n.state);
  if (!t && !s)
    return !1;
  sr = s ? t : null, e.type == "cut" && !n.state.readOnly && n.dispatch({
    changes: i,
    scrollIntoView: !0,
    userEvent: "delete.cut"
  });
  let r = ja ? null : e.clipboardData;
  return r ? (r.clearData(), r.setData("text/plain", t), !0) : (Cu(n, t), !1);
};
const Ga = /* @__PURE__ */ yt.define();
function Ja(n, e) {
  let t = [];
  for (let i of n.facet(Oa)) {
    let s = i(n, e);
    s && t.push(s);
  }
  return t.length ? n.update({ effects: t, annotations: Ga.of(!0) }) : null;
}
function Ya(n) {
  setTimeout(() => {
    let e = n.hasFocus;
    if (e != n.inputState.notifiedFocused) {
      let t = Ja(n.state, e);
      t ? n.dispatch(t) : n.update([]);
    }
  }, 10);
}
ye.focus = (n) => {
  n.inputState.lastFocusTime = Date.now(), !n.scrollDOM.scrollTop && (n.inputState.lastScrollTop || n.inputState.lastScrollLeft) && (n.scrollDOM.scrollTop = n.inputState.lastScrollTop, n.scrollDOM.scrollLeft = n.inputState.lastScrollLeft), Ya(n);
};
ye.blur = (n) => {
  n.observer.clearSelectionRange(), Ya(n);
};
ye.compositionstart = ye.compositionupdate = (n) => {
  n.observer.editContext || (n.inputState.compositionFirstChange == null && (n.inputState.compositionFirstChange = !0), n.inputState.composing < 0 && (n.inputState.composing = 0));
};
ye.compositionend = (n) => {
  n.observer.editContext || (n.inputState.composing = -1, n.inputState.compositionEndedAt = Date.now(), n.inputState.compositionPendingKey = !0, n.inputState.compositionPendingChange = n.observer.pendingRecords().length > 0, n.inputState.compositionFirstChange = null, C.chrome && C.android ? n.observer.flushSoon() : n.inputState.compositionPendingChange ? Promise.resolve().then(() => n.observer.flush()) : setTimeout(() => {
    n.inputState.composing < 0 && n.docView.hasComposition && n.update([]);
  }, 50));
};
ye.contextmenu = (n) => {
  n.inputState.lastContextMenu = Date.now();
};
$e.beforeinput = (n, e) => {
  var t, i;
  if ((e.inputType == "insertText" || e.inputType == "insertCompositionText") && (n.inputState.insertingText = e.data, n.inputState.insertingTextAt = Date.now()), e.inputType == "insertReplacementText" && n.observer.editContext) {
    let r = (t = e.dataTransfer) === null || t === void 0 ? void 0 : t.getData("text/plain"), o = e.getTargetRanges();
    if (r && o.length) {
      let l = o[0], a = n.posAtDOM(l.startContainer, l.startOffset), h = n.posAtDOM(l.endContainer, l.endOffset);
      return Ir(n, { from: a, to: h, insert: n.state.toText(r) }, null), !0;
    }
  }
  let s;
  if (C.chrome && C.android && (s = za.find((r) => r.inputType == e.inputType)) && (n.observer.delayAndroidKey(s.key, s.keyCode), s.key == "Backspace" || s.key == "Delete")) {
    let r = ((i = window.visualViewport) === null || i === void 0 ? void 0 : i.height) || 0;
    setTimeout(() => {
      var o;
      (((o = window.visualViewport) === null || o === void 0 ? void 0 : o.height) || 0) > r + 10 && n.hasFocus && (n.contentDOM.blur(), n.focus());
    }, 100);
  }
  return C.ios && e.inputType == "deleteContentForward" && n.observer.flushSoon(), C.safari && e.inputType == "insertText" && n.inputState.composing >= 0 && setTimeout(() => ye.compositionend(n, e), 20), !1;
};
const Wo = /* @__PURE__ */ new Set();
function Tu(n) {
  Wo.has(n) || (Wo.add(n), n.addEventListener("copy", () => {
  }), n.addEventListener("cut", () => {
  }));
}
const _o = ["pre-wrap", "normal", "pre-line", "break-spaces"];
let ei = !1;
function zo() {
  ei = !1;
}
class Ou {
  constructor(e) {
    this.lineWrapping = e, this.doc = $.empty, this.heightSamples = {}, this.lineHeight = 14, this.charWidth = 7, this.textHeight = 14, this.lineLength = 30;
  }
  heightForGap(e, t) {
    let i = this.doc.lineAt(t).number - this.doc.lineAt(e).number + 1;
    return this.lineWrapping && (i += Math.max(0, Math.ceil((t - e - i * this.lineLength * 0.5) / this.lineLength))), this.lineHeight * i;
  }
  heightForLine(e) {
    return this.lineWrapping ? (1 + Math.max(0, Math.ceil((e - this.lineLength) / Math.max(1, this.lineLength - 5)))) * this.lineHeight : this.lineHeight;
  }
  setDoc(e) {
    return this.doc = e, this;
  }
  mustRefreshForWrapping(e) {
    return _o.indexOf(e) > -1 != this.lineWrapping;
  }
  mustRefreshForHeights(e) {
    let t = !1;
    for (let i = 0; i < e.length; i++) {
      let s = e[i];
      s < 0 ? i++ : this.heightSamples[Math.floor(s * 10)] || (t = !0, this.heightSamples[Math.floor(s * 10)] = !0);
    }
    return t;
  }
  refresh(e, t, i, s, r, o) {
    let l = _o.indexOf(e) > -1, a = Math.abs(t - this.lineHeight) > 0.3 || this.lineWrapping != l;
    if (this.lineWrapping = l, this.lineHeight = t, this.charWidth = i, this.textHeight = s, this.lineLength = r, a) {
      this.heightSamples = {};
      for (let h = 0; h < o.length; h++) {
        let c = o[h];
        c < 0 ? h++ : this.heightSamples[Math.floor(c * 10)] = !0;
      }
    }
    return a;
  }
}
class Du {
  constructor(e, t) {
    this.from = e, this.heights = t, this.index = 0;
  }
  get more() {
    return this.index < this.heights.length;
  }
}
class Re {
  /**
  @internal
  */
  constructor(e, t, i, s, r) {
    this.from = e, this.length = t, this.top = i, this.height = s, this._content = r;
  }
  /**
  The type of element this is. When querying lines, this may be
  an array of all the blocks that make up the line.
  */
  get type() {
    return typeof this._content == "number" ? re.Text : Array.isArray(this._content) ? this._content : this._content.type;
  }
  /**
  The end of the element as a document position.
  */
  get to() {
    return this.from + this.length;
  }
  /**
  The bottom position of the element.
  */
  get bottom() {
    return this.top + this.height;
  }
  /**
  If this is a widget block, this will return the widget
  associated with it.
  */
  get widget() {
    return this._content instanceof Bt ? this._content.widget : null;
  }
  /**
  If this is a textblock, this holds the number of line breaks
  that appear in widgets inside the block.
  */
  get widgetLineBreaks() {
    return typeof this._content == "number" ? this._content : 0;
  }
  /**
  @internal
  */
  join(e) {
    let t = (Array.isArray(this._content) ? this._content : [this]).concat(Array.isArray(e._content) ? e._content : [e]);
    return new Re(this.from, this.length + e.length, this.top, this.height + e.height, t);
  }
}
var j = /* @__PURE__ */ (function(n) {
  return n[n.ByPos = 0] = "ByPos", n[n.ByHeight = 1] = "ByHeight", n[n.ByPosNoHeight = 2] = "ByPosNoHeight", n;
})(j || (j = {}));
const vn = 1e-3;
class de {
  constructor(e, t, i = 2) {
    this.length = e, this.height = t, this.flags = i;
  }
  get outdated() {
    return (this.flags & 2) > 0;
  }
  set outdated(e) {
    this.flags = (e ? 2 : 0) | this.flags & -3;
  }
  setHeight(e) {
    this.height != e && (Math.abs(this.height - e) > vn && (ei = !0), this.height = e);
  }
  // Base case is to replace a leaf node, which simply builds a tree
  // from the new nodes and returns that (HeightMapBranch and
  // HeightMapGap override this to actually use from/to)
  replace(e, t, i) {
    return de.of(i);
  }
  // Again, these are base cases, and are overridden for branch and gap nodes.
  decomposeLeft(e, t) {
    t.push(this);
  }
  decomposeRight(e, t) {
    t.push(this);
  }
  applyChanges(e, t, i, s) {
    let r = this, o = i.doc;
    for (let l = s.length - 1; l >= 0; l--) {
      let { fromA: a, toA: h, fromB: c, toB: f } = s[l], u = r.lineAt(a, j.ByPosNoHeight, i.setDoc(t), 0, 0), d = u.to >= h ? u : r.lineAt(h, j.ByPosNoHeight, i, 0, 0);
      for (f += d.to - h, h = d.to; l > 0 && u.from <= s[l - 1].toA; )
        a = s[l - 1].fromA, c = s[l - 1].fromB, l--, a < u.from && (u = r.lineAt(a, j.ByPosNoHeight, i, 0, 0));
      c += u.from - a, a = u.from;
      let p = $r.build(i.setDoc(o), e, c, f);
      r = In(r, r.replace(a, h, p));
    }
    return r.updateHeight(i, 0);
  }
  static empty() {
    return new ke(0, 0, 0);
  }
  // nodes uses null values to indicate the position of line breaks.
  // There are never line breaks at the start or end of the array, or
  // two line breaks next to each other, and the array isn't allowed
  // to be empty (same restrictions as return value from the builder).
  static of(e) {
    if (e.length == 1)
      return e[0];
    let t = 0, i = e.length, s = 0, r = 0;
    for (; ; )
      if (t == i)
        if (s > r * 2) {
          let l = e[t - 1];
          l.break ? e.splice(--t, 1, l.left, null, l.right) : e.splice(--t, 1, l.left, l.right), i += 1 + l.break, s -= l.size;
        } else if (r > s * 2) {
          let l = e[i];
          l.break ? e.splice(i, 1, l.left, null, l.right) : e.splice(i, 1, l.left, l.right), i += 2 + l.break, r -= l.size;
        } else
          break;
      else if (s < r) {
        let l = e[t++];
        l && (s += l.size);
      } else {
        let l = e[--i];
        l && (r += l.size);
      }
    let o = 0;
    return e[t - 1] == null ? (o = 1, t--) : e[t] == null && (o = 1, i++), new Lu(de.of(e.slice(0, t)), o, de.of(e.slice(i)));
  }
}
function In(n, e) {
  return n == e ? n : (n.constructor != e.constructor && (ei = !0), e);
}
de.prototype.size = 1;
const Eu = /* @__PURE__ */ K.replace({});
class Xa extends de {
  constructor(e, t, i) {
    super(e, t), this.deco = i, this.spaceAbove = 0;
  }
  mainBlock(e, t) {
    return new Re(t, this.length, e + this.spaceAbove, this.height - this.spaceAbove, this.deco || 0);
  }
  blockAt(e, t, i, s) {
    return this.spaceAbove && e < i + this.spaceAbove ? new Re(s, 0, i, this.spaceAbove, Eu) : this.mainBlock(i, s);
  }
  lineAt(e, t, i, s, r) {
    let o = this.mainBlock(s, r);
    return this.spaceAbove ? this.blockAt(0, i, s, r).join(o) : o;
  }
  forEachLine(e, t, i, s, r, o) {
    e <= r + this.length && t >= r && o(this.lineAt(0, j.ByPos, i, s, r));
  }
  setMeasuredHeight(e) {
    let t = e.heights[e.index++];
    t < 0 ? (this.spaceAbove = -t, t = e.heights[e.index++]) : this.spaceAbove = 0, this.setHeight(t);
  }
  updateHeight(e, t = 0, i = !1, s) {
    return s && s.from <= t && s.more && this.setMeasuredHeight(s), this.outdated = !1, this;
  }
  toString() {
    return `block(${this.length})`;
  }
}
class ke extends Xa {
  constructor(e, t, i) {
    super(e, t, null), this.collapsed = 0, this.widgetHeight = 0, this.breaks = 0, this.spaceAbove = i;
  }
  mainBlock(e, t) {
    return new Re(t, this.length, e + this.spaceAbove, this.height - this.spaceAbove, this.breaks);
  }
  replace(e, t, i) {
    let s = i[0];
    return i.length == 1 && (s instanceof ke || s instanceof ne && s.flags & 4) && Math.abs(this.length - s.length) < 10 ? (s instanceof ne ? s = new ke(s.length, this.height, this.spaceAbove) : s.height = this.height, this.outdated || (s.outdated = !1), s) : de.of(i);
  }
  updateHeight(e, t = 0, i = !1, s) {
    return s && s.from <= t && s.more ? this.setMeasuredHeight(s) : (i || this.outdated) && (this.spaceAbove = 0, this.setHeight(Math.max(this.widgetHeight, e.heightForLine(this.length - this.collapsed)) + this.breaks * e.lineHeight)), this.outdated = !1, this;
  }
  toString() {
    return `line(${this.length}${this.collapsed ? -this.collapsed : ""}${this.widgetHeight ? ":" + this.widgetHeight : ""})`;
  }
}
class ne extends de {
  constructor(e) {
    super(e, 0);
  }
  heightMetrics(e, t) {
    let i = e.doc.lineAt(t).number, s = e.doc.lineAt(t + this.length).number, r = s - i + 1, o, l = 0;
    if (e.lineWrapping) {
      let a = Math.min(this.height, e.lineHeight * r);
      o = a / r, this.length > r + 1 && (l = (this.height - a) / (this.length - r - 1));
    } else
      o = this.height / r;
    return { firstLine: i, lastLine: s, perLine: o, perChar: l };
  }
  blockAt(e, t, i, s) {
    let { firstLine: r, lastLine: o, perLine: l, perChar: a } = this.heightMetrics(t, s);
    if (t.lineWrapping) {
      let h = s + (e < t.lineHeight ? 0 : Math.round(Math.max(0, Math.min(1, (e - i) / this.height)) * this.length)), c = t.doc.lineAt(h), f = l + c.length * a, u = Math.max(i, e - f / 2);
      return new Re(c.from, c.length, u, f, 0);
    } else {
      let h = Math.max(0, Math.min(o - r, Math.floor((e - i) / l))), { from: c, length: f } = t.doc.line(r + h);
      return new Re(c, f, i + l * h, l, 0);
    }
  }
  lineAt(e, t, i, s, r) {
    if (t == j.ByHeight)
      return this.blockAt(e, i, s, r);
    if (t == j.ByPosNoHeight) {
      let { from: d, to: p } = i.doc.lineAt(e);
      return new Re(d, p - d, 0, 0, 0);
    }
    let { firstLine: o, perLine: l, perChar: a } = this.heightMetrics(i, r), h = i.doc.lineAt(e), c = l + h.length * a, f = h.number - o, u = s + l * f + a * (h.from - r - f);
    return new Re(h.from, h.length, Math.max(s, Math.min(u, s + this.height - c)), c, 0);
  }
  forEachLine(e, t, i, s, r, o) {
    e = Math.max(e, r), t = Math.min(t, r + this.length);
    let { firstLine: l, perLine: a, perChar: h } = this.heightMetrics(i, r);
    for (let c = e, f = s; c <= t; ) {
      let u = i.doc.lineAt(c);
      if (c == e) {
        let p = u.number - l;
        f += a * p + h * (e - r - p);
      }
      let d = a + h * u.length;
      o(new Re(u.from, u.length, f, d, 0)), f += d, c = u.to + 1;
    }
  }
  replace(e, t, i) {
    let s = this.length - t;
    if (s > 0) {
      let r = i[i.length - 1];
      r instanceof ne ? i[i.length - 1] = new ne(r.length + s) : i.push(null, new ne(s - 1));
    }
    if (e > 0) {
      let r = i[0];
      r instanceof ne ? i[0] = new ne(e + r.length) : i.unshift(new ne(e - 1), null);
    }
    return de.of(i);
  }
  decomposeLeft(e, t) {
    t.push(new ne(e - 1), null);
  }
  decomposeRight(e, t) {
    t.push(null, new ne(this.length - e - 1));
  }
  updateHeight(e, t = 0, i = !1, s) {
    let r = t + this.length;
    if (s && s.from <= t + this.length && s.more) {
      let o = [], l = Math.max(t, s.from), a = -1;
      for (s.from > t && o.push(new ne(s.from - t - 1).updateHeight(e, t)); l <= r && s.more; ) {
        let c = e.doc.lineAt(l).length;
        o.length && o.push(null);
        let f = s.heights[s.index++], u = 0;
        f < 0 && (u = -f, f = s.heights[s.index++]), a == -1 ? a = f : Math.abs(f - a) >= vn && (a = -2);
        let d = new ke(c, f, u);
        d.outdated = !1, o.push(d), l += c + 1;
      }
      l <= r && o.push(null, new ne(r - l).updateHeight(e, l));
      let h = de.of(o);
      return (a < 0 || Math.abs(h.height - this.height) >= vn || Math.abs(a - this.heightMetrics(e, t).perLine) >= vn) && (ei = !0), In(this, h);
    } else (i || this.outdated) && (this.setHeight(e.heightForGap(t, t + this.length)), this.outdated = !1);
    return this;
  }
  toString() {
    return `gap(${this.length})`;
  }
}
class Lu extends de {
  constructor(e, t, i) {
    super(e.length + t + i.length, e.height + i.height, t | (e.outdated || i.outdated ? 2 : 0)), this.left = e, this.right = i, this.size = e.size + i.size;
  }
  get break() {
    return this.flags & 1;
  }
  blockAt(e, t, i, s) {
    let r = i + this.left.height;
    return e < r ? this.left.blockAt(e, t, i, s) : this.right.blockAt(e, t, r, s + this.left.length + this.break);
  }
  lineAt(e, t, i, s, r) {
    let o = s + this.left.height, l = r + this.left.length + this.break, a = t == j.ByHeight ? e < o : e < l, h = a ? this.left.lineAt(e, t, i, s, r) : this.right.lineAt(e, t, i, o, l);
    if (this.break || (a ? h.to < l : h.from > l))
      return h;
    let c = t == j.ByPosNoHeight ? j.ByPosNoHeight : j.ByPos;
    return a ? h.join(this.right.lineAt(l, c, i, o, l)) : this.left.lineAt(l, c, i, s, r).join(h);
  }
  forEachLine(e, t, i, s, r, o) {
    let l = s + this.left.height, a = r + this.left.length + this.break;
    if (this.break)
      e < a && this.left.forEachLine(e, t, i, s, r, o), t >= a && this.right.forEachLine(e, t, i, l, a, o);
    else {
      let h = this.lineAt(a, j.ByPos, i, s, r);
      e < h.from && this.left.forEachLine(e, h.from - 1, i, s, r, o), h.to >= e && h.from <= t && o(h), t > h.to && this.right.forEachLine(h.to + 1, t, i, l, a, o);
    }
  }
  replace(e, t, i) {
    let s = this.left.length + this.break;
    if (t < s)
      return this.balanced(this.left.replace(e, t, i), this.right);
    if (e > this.left.length)
      return this.balanced(this.left, this.right.replace(e - s, t - s, i));
    let r = [];
    e > 0 && this.decomposeLeft(e, r);
    let o = r.length;
    for (let l of i)
      r.push(l);
    if (e > 0 && Uo(r, o - 1), t < this.length) {
      let l = r.length;
      this.decomposeRight(t, r), Uo(r, l);
    }
    return de.of(r);
  }
  decomposeLeft(e, t) {
    let i = this.left.length;
    if (e <= i)
      return this.left.decomposeLeft(e, t);
    t.push(this.left), this.break && (i++, e >= i && t.push(null)), e > i && this.right.decomposeLeft(e - i, t);
  }
  decomposeRight(e, t) {
    let i = this.left.length, s = i + this.break;
    if (e >= s)
      return this.right.decomposeRight(e - s, t);
    e < i && this.left.decomposeRight(e, t), this.break && e < s && t.push(null), t.push(this.right);
  }
  balanced(e, t) {
    return e.size > 2 * t.size || t.size > 2 * e.size ? de.of(this.break ? [e, null, t] : [e, t]) : (this.left = In(this.left, e), this.right = In(this.right, t), this.setHeight(e.height + t.height), this.outdated = e.outdated || t.outdated, this.size = e.size + t.size, this.length = e.length + this.break + t.length, this);
  }
  updateHeight(e, t = 0, i = !1, s) {
    let { left: r, right: o } = this, l = t + r.length + this.break, a = null;
    return s && s.from <= t + r.length && s.more ? a = r = r.updateHeight(e, t, i, s) : r.updateHeight(e, t, i), s && s.from <= l + o.length && s.more ? a = o = o.updateHeight(e, l, i, s) : o.updateHeight(e, l, i), a ? this.balanced(r, o) : (this.height = this.left.height + this.right.height, this.outdated = !1, this);
  }
  toString() {
    return this.left + (this.break ? " " : "-") + this.right;
  }
}
function Uo(n, e) {
  let t, i;
  n[e] == null && (t = n[e - 1]) instanceof ne && (i = n[e + 1]) instanceof ne && n.splice(e - 1, 3, new ne(t.length + 1 + i.length));
}
const Ru = 5;
class $r {
  constructor(e, t) {
    this.pos = e, this.oracle = t, this.nodes = [], this.lineStart = -1, this.lineEnd = -1, this.covering = null, this.writtenTo = e;
  }
  get isCovered() {
    return this.covering && this.nodes[this.nodes.length - 1] == this.covering;
  }
  span(e, t) {
    if (this.lineStart > -1) {
      let i = Math.min(t, this.lineEnd), s = this.nodes[this.nodes.length - 1];
      s instanceof ke ? s.length += i - this.pos : (i > this.pos || !this.isCovered) && this.nodes.push(new ke(i - this.pos, -1, 0)), this.writtenTo = i, t > i && (this.nodes.push(null), this.writtenTo++, this.lineStart = -1);
    }
    this.pos = t;
  }
  point(e, t, i) {
    if (e < t || i.heightRelevant) {
      let s = i.widget ? i.widget.estimatedHeight : 0, r = i.widget ? i.widget.lineBreaks : 0;
      s < 0 && (s = this.oracle.lineHeight);
      let o = t - e;
      i.block ? this.addBlock(new Xa(o, s, i)) : (o || r || s >= Ru) && this.addLineDeco(s, r, o);
    } else t > e && this.span(e, t);
    this.lineEnd > -1 && this.lineEnd < this.pos && (this.lineEnd = this.oracle.doc.lineAt(this.pos).to);
  }
  enterLine() {
    if (this.lineStart > -1)
      return;
    let { from: e, to: t } = this.oracle.doc.lineAt(this.pos);
    this.lineStart = e, this.lineEnd = t, this.writtenTo < e && ((this.writtenTo < e - 1 || this.nodes[this.nodes.length - 1] == null) && this.nodes.push(this.blankContent(this.writtenTo, e - 1)), this.nodes.push(null)), this.pos > e && this.nodes.push(new ke(this.pos - e, -1, 0)), this.writtenTo = this.pos;
  }
  blankContent(e, t) {
    let i = new ne(t - e);
    return this.oracle.doc.lineAt(e).to == t && (i.flags |= 4), i;
  }
  ensureLine() {
    this.enterLine();
    let e = this.nodes.length ? this.nodes[this.nodes.length - 1] : null;
    if (e instanceof ke)
      return e;
    let t = new ke(0, -1, 0);
    return this.nodes.push(t), t;
  }
  addBlock(e) {
    this.enterLine();
    let t = e.deco;
    t && t.startSide > 0 && !this.isCovered && this.ensureLine(), this.nodes.push(e), this.writtenTo = this.pos = this.pos + e.length, t && t.endSide > 0 && (this.covering = e);
  }
  addLineDeco(e, t, i) {
    let s = this.ensureLine();
    s.length += i, s.collapsed += i, s.widgetHeight = Math.max(s.widgetHeight, e), s.breaks += t, this.writtenTo = this.pos = this.pos + i;
  }
  finish(e) {
    let t = this.nodes.length == 0 ? null : this.nodes[this.nodes.length - 1];
    this.lineStart > -1 && !(t instanceof ke) && !this.isCovered ? this.nodes.push(new ke(0, -1, 0)) : (this.writtenTo < this.pos || t == null) && this.nodes.push(this.blankContent(this.writtenTo, this.pos));
    let i = e;
    for (let s of this.nodes)
      s instanceof ke && s.updateHeight(this.oracle, i), i += s ? s.length : 1;
    return this.nodes;
  }
  // Always called with a region that on both sides either stretches
  // to a line break or the end of the document.
  // The returned array uses null to indicate line breaks, but never
  // starts or ends in a line break, or has multiple line breaks next
  // to each other.
  static build(e, t, i, s) {
    let r = new $r(i, e);
    return B.spans(t, i, s, r, 0), r.finish(i);
  }
}
function Bu(n, e, t) {
  let i = new Pu();
  return B.compare(n, e, t, i, 0), i.changes;
}
class Pu {
  constructor() {
    this.changes = [];
  }
  compareRange() {
  }
  comparePoint(e, t, i, s) {
    (e < t || i && i.heightRelevant || s && s.heightRelevant) && qt(e, t, this.changes, 5);
  }
}
function Iu(n, e) {
  let t = n.getBoundingClientRect(), i = n.ownerDocument, s = i.defaultView || window, r = Math.max(0, t.left), o = Math.min(s.innerWidth, t.right), l = Math.max(0, t.top), a = Math.min(s.innerHeight, t.bottom);
  for (let h = n.parentNode; h && h != i.body; )
    if (h.nodeType == 1) {
      let c = h, f = window.getComputedStyle(c);
      if ((c.scrollHeight > c.clientHeight || c.scrollWidth > c.clientWidth) && f.overflow != "visible") {
        let u = c.getBoundingClientRect();
        r = Math.max(r, u.left), o = Math.min(o, u.right), l = Math.max(l, u.top), a = Math.min(h == n.parentNode ? s.innerHeight : a, u.bottom);
      }
      h = f.position == "absolute" || f.position == "fixed" ? c.offsetParent : c.parentNode;
    } else if (h.nodeType == 11)
      h = h.host;
    else
      break;
  return {
    left: r - t.left,
    right: Math.max(r, o) - t.left,
    top: l - (t.top + e),
    bottom: Math.max(l, a) - (t.top + e)
  };
}
function $u(n) {
  let e = n.getBoundingClientRect(), t = n.ownerDocument.defaultView || window;
  return e.left < t.innerWidth && e.right > 0 && e.top < t.innerHeight && e.bottom > 0;
}
function Nu(n, e) {
  let t = n.getBoundingClientRect();
  return {
    left: 0,
    right: t.right - t.left,
    top: e,
    bottom: t.bottom - (t.top + e)
  };
}
class bs {
  constructor(e, t, i, s) {
    this.from = e, this.to = t, this.size = i, this.displaySize = s;
  }
  static same(e, t) {
    if (e.length != t.length)
      return !1;
    for (let i = 0; i < e.length; i++) {
      let s = e[i], r = t[i];
      if (s.from != r.from || s.to != r.to || s.size != r.size)
        return !1;
    }
    return !0;
  }
  draw(e, t) {
    return K.replace({
      widget: new Hu(this.displaySize * (t ? e.scaleY : e.scaleX), t)
    }).range(this.from, this.to);
  }
}
class Hu extends zi {
  constructor(e, t) {
    super(), this.size = e, this.vertical = t;
  }
  eq(e) {
    return e.size == this.size && e.vertical == this.vertical;
  }
  toDOM() {
    let e = document.createElement("div");
    return this.vertical ? e.style.height = this.size + "px" : (e.style.width = this.size + "px", e.style.height = "2px", e.style.display = "inline-block"), e;
  }
  get estimatedHeight() {
    return this.vertical ? this.size : -1;
  }
}
class jo {
  constructor(e, t) {
    this.view = e, this.state = t, this.pixelViewport = { left: 0, right: window.innerWidth, top: 0, bottom: 0 }, this.inView = !0, this.paddingTop = 0, this.paddingBottom = 0, this.contentDOMWidth = 0, this.contentDOMHeight = 0, this.editorHeight = 0, this.editorWidth = 0, this.scaleX = 1, this.scaleY = 1, this.scrollOffset = 0, this.scrolledToBottom = !1, this.scrollAnchorPos = 0, this.scrollAnchorHeight = -1, this.scaler = qo, this.scrollTarget = null, this.printing = !1, this.mustMeasureContent = !0, this.defaultTextDirection = q.LTR, this.visibleRanges = [], this.mustEnforceCursorAssoc = !1;
    let i = t.facet(Rr).some((s) => typeof s != "function" && s.class == "cm-lineWrapping");
    this.heightOracle = new Ou(i), this.stateDeco = Ko(t), this.heightMap = de.empty().applyChanges(this.stateDeco, $.empty, this.heightOracle.setDoc(t.doc), [new Oe(0, 0, 0, t.doc.length)]);
    for (let s = 0; s < 2 && (this.viewport = this.getViewport(0, null), !!this.updateForViewport()); s++)
      ;
    this.updateViewportLines(), this.lineGaps = this.ensureLineGaps([]), this.lineGapDeco = K.set(this.lineGaps.map((s) => s.draw(this, !1))), this.scrollParent = e.scrollDOM, this.computeVisibleRanges();
  }
  updateForViewport() {
    let e = [this.viewport], { main: t } = this.state.selection;
    for (let i = 0; i <= 1; i++) {
      let s = i ? t.head : t.anchor;
      if (!e.some(({ from: r, to: o }) => s >= r && s <= o)) {
        let { from: r, to: o } = this.lineBlockAt(s);
        e.push(new nn(r, o));
      }
    }
    return this.viewports = e.sort((i, s) => i.from - s.from), this.updateScaler();
  }
  updateScaler() {
    let e = this.scaler;
    return this.scaler = this.heightMap.height <= 7e6 ? qo : new Nr(this.heightOracle, this.heightMap, this.viewports), e.eq(this.scaler) ? 0 : 2;
  }
  updateViewportLines() {
    this.viewportLines = [], this.heightMap.forEachLine(this.viewport.from, this.viewport.to, this.heightOracle.setDoc(this.state.doc), 0, 0, (e) => {
      this.viewportLines.push(bi(e, this.scaler));
    });
  }
  update(e, t = null) {
    this.state = e.state;
    let i = this.stateDeco;
    this.stateDeco = Ko(this.state);
    let s = e.changedRanges, r = Oe.extendWithRanges(s, Bu(i, this.stateDeco, e ? e.changes : ee.empty(this.state.doc.length))), o = this.heightMap.height, l = this.scrolledToBottom ? null : this.scrollAnchorAt(this.scrollOffset);
    zo(), this.heightMap = this.heightMap.applyChanges(this.stateDeco, e.startState.doc, this.heightOracle.setDoc(this.state.doc), r), (this.heightMap.height != o || ei) && (e.flags |= 2), l ? (this.scrollAnchorPos = e.changes.mapPos(l.from, -1), this.scrollAnchorHeight = l.top) : (this.scrollAnchorPos = -1, this.scrollAnchorHeight = o);
    let a = r.length ? this.mapViewport(this.viewport, e.changes) : this.viewport;
    (t && (t.range.head < a.from || t.range.head > a.to) || !this.viewportIsAppropriate(a)) && (a = this.getViewport(0, t));
    let h = a.from != this.viewport.from || a.to != this.viewport.to;
    this.viewport = a, e.flags |= this.updateForViewport(), (h || !e.changes.empty || e.flags & 2) && this.updateViewportLines(), (this.lineGaps.length || this.viewport.to - this.viewport.from > 4e3) && this.updateLineGaps(this.ensureLineGaps(this.mapLineGaps(this.lineGaps, e.changes))), e.flags |= this.computeVisibleRanges(e.changes), t && (this.scrollTarget = t), !this.mustEnforceCursorAssoc && (e.selectionSet || e.focusChanged) && e.view.lineWrapping && e.state.selection.main.empty && e.state.selection.main.assoc && !e.state.facet(Ea) && (this.mustEnforceCursorAssoc = !0);
  }
  measure() {
    let { view: e } = this, t = e.contentDOM, i = window.getComputedStyle(t), s = this.heightOracle, r = i.whiteSpace;
    this.defaultTextDirection = i.direction == "rtl" ? q.RTL : q.LTR;
    let o = this.heightOracle.mustRefreshForWrapping(r) || this.mustMeasureContent === "refresh", l = t.getBoundingClientRect(), a = o || this.mustMeasureContent || this.contentDOMHeight != l.height;
    this.contentDOMHeight = l.height, this.mustMeasureContent = !1;
    let h = 0, c = 0;
    if (l.width && l.height) {
      let { scaleX: k, scaleY: S } = ua(t, l);
      (k > 5e-3 && Math.abs(this.scaleX - k) > 5e-3 || S > 5e-3 && Math.abs(this.scaleY - S) > 5e-3) && (this.scaleX = k, this.scaleY = S, h |= 16, o = a = !0);
    }
    let f = (parseInt(i.paddingTop) || 0) * this.scaleY, u = (parseInt(i.paddingBottom) || 0) * this.scaleY;
    (this.paddingTop != f || this.paddingBottom != u) && (this.paddingTop = f, this.paddingBottom = u, h |= 18), this.editorWidth != e.scrollDOM.clientWidth && (s.lineWrapping && (a = !0), this.editorWidth = e.scrollDOM.clientWidth, h |= 16);
    let d = da(this.view.contentDOM, !1).y;
    d != this.scrollParent && (this.scrollParent = d, this.scrollAnchorHeight = -1, this.scrollOffset = 0);
    let p = this.getScrollOffset();
    this.scrollOffset != p && (this.scrollAnchorHeight = -1, this.scrollOffset = p), this.scrolledToBottom = ga(this.scrollParent || e.win);
    let g = (this.printing ? Nu : Iu)(t, this.paddingTop), m = g.top - this.pixelViewport.top, b = g.bottom - this.pixelViewport.bottom;
    this.pixelViewport = g;
    let v = this.pixelViewport.bottom > this.pixelViewport.top && this.pixelViewport.right > this.pixelViewport.left;
    if (v != this.inView && (this.inView = v, v && (a = !0)), !this.inView && !this.scrollTarget && !$u(e.dom))
      return 0;
    let w = l.width;
    if ((this.contentDOMWidth != w || this.editorHeight != e.scrollDOM.clientHeight) && (this.contentDOMWidth = l.width, this.editorHeight = e.scrollDOM.clientHeight, h |= 16), a) {
      let k = e.docView.measureVisibleLineHeights(this.viewport);
      if (s.mustRefreshForHeights(k) && (o = !0), o || s.lineWrapping && Math.abs(w - this.contentDOMWidth) > s.charWidth) {
        let { lineHeight: S, charWidth: A, textHeight: L } = e.docView.measureTextSize();
        o = S > 0 && s.refresh(r, S, A, L, Math.max(5, w / A), k), o && (e.docView.minWidth = 0, h |= 16);
      }
      m > 0 && b > 0 ? c = Math.max(m, b) : m < 0 && b < 0 && (c = Math.min(m, b)), zo();
      for (let S of this.viewports) {
        let A = S.from == this.viewport.from ? k : e.docView.measureVisibleLineHeights(S);
        this.heightMap = (o ? de.empty().applyChanges(this.stateDeco, $.empty, this.heightOracle, [new Oe(0, 0, 0, e.state.doc.length)]) : this.heightMap).updateHeight(s, 0, o, new Du(S.from, A));
      }
      ei && (h |= 2);
    }
    let O = !this.viewportIsAppropriate(this.viewport, c) || this.scrollTarget && (this.scrollTarget.range.head < this.viewport.from || this.scrollTarget.range.head > this.viewport.to);
    return O && (h & 2 && (h |= this.updateScaler()), this.viewport = this.getViewport(c, this.scrollTarget), h |= this.updateForViewport()), (h & 2 || O) && this.updateViewportLines(), (this.lineGaps.length || this.viewport.to - this.viewport.from > 4e3) && this.updateLineGaps(this.ensureLineGaps(o ? [] : this.lineGaps, e)), h |= this.computeVisibleRanges(), this.mustEnforceCursorAssoc && (this.mustEnforceCursorAssoc = !1, e.docView.enforceCursorAssoc()), h;
  }
  get visibleTop() {
    return this.scaler.fromDOM(this.pixelViewport.top);
  }
  get visibleBottom() {
    return this.scaler.fromDOM(this.pixelViewport.bottom);
  }
  getViewport(e, t) {
    let i = 0.5 - Math.max(-0.5, Math.min(0.5, e / 1e3 / 2)), s = this.heightMap, r = this.heightOracle, { visibleTop: o, visibleBottom: l } = this, a = new nn(s.lineAt(o - i * 1e3, j.ByHeight, r, 0, 0).from, s.lineAt(l + (1 - i) * 1e3, j.ByHeight, r, 0, 0).to);
    if (t) {
      let { head: h } = t.range;
      if (h < a.from || h > a.to) {
        let c = Math.min(this.editorHeight, this.pixelViewport.bottom - this.pixelViewport.top), f = s.lineAt(h, j.ByPos, r, 0, 0), u;
        t.y == "center" ? u = (f.top + f.bottom) / 2 - c / 2 : t.y == "start" || t.y == "nearest" && h < a.from ? u = f.top : u = f.bottom - c, a = new nn(s.lineAt(u - 1e3 / 2, j.ByHeight, r, 0, 0).from, s.lineAt(u + c + 1e3 / 2, j.ByHeight, r, 0, 0).to);
      }
    }
    return a;
  }
  mapViewport(e, t) {
    let i = t.mapPos(e.from, -1), s = t.mapPos(e.to, 1);
    return new nn(this.heightMap.lineAt(i, j.ByPos, this.heightOracle, 0, 0).from, this.heightMap.lineAt(s, j.ByPos, this.heightOracle, 0, 0).to);
  }
  // Checks if a given viewport covers the visible part of the
  // document and not too much beyond that.
  viewportIsAppropriate({ from: e, to: t }, i = 0) {
    if (!this.inView)
      return !0;
    let { top: s } = this.heightMap.lineAt(e, j.ByPos, this.heightOracle, 0, 0), { bottom: r } = this.heightMap.lineAt(t, j.ByPos, this.heightOracle, 0, 0), { visibleTop: o, visibleBottom: l } = this;
    return (e == 0 || s <= o - Math.max(10, Math.min(
      -i,
      250
      /* VP.MaxCoverMargin */
    ))) && (t == this.state.doc.length || r >= l + Math.max(10, Math.min(
      i,
      250
      /* VP.MaxCoverMargin */
    ))) && s > o - 2 * 1e3 && r < l + 2 * 1e3;
  }
  mapLineGaps(e, t) {
    if (!e.length || t.empty)
      return e;
    let i = [];
    for (let s of e)
      t.touchesRange(s.from, s.to) || i.push(new bs(t.mapPos(s.from), t.mapPos(s.to), s.size, s.displaySize));
    return i;
  }
  // Computes positions in the viewport where the start or end of a
  // line should be hidden, trying to reuse existing line gaps when
  // appropriate to avoid unneccesary redraws.
  // Uses crude character-counting for the positioning and sizing,
  // since actual DOM coordinates aren't always available and
  // predictable. Relies on generous margins (see LG.Margin) to hide
  // the artifacts this might produce from the user.
  ensureLineGaps(e, t) {
    let i = this.heightOracle.lineWrapping, s = i ? 1e4 : 2e3, r = s >> 1, o = s << 1;
    if (this.defaultTextDirection != q.LTR && !i)
      return [];
    let l = [], a = (c, f, u, d) => {
      if (f - c < r)
        return;
      let p = this.state.selection.main, g = [p.from];
      p.empty || g.push(p.to);
      for (let b of g)
        if (b > c && b < f) {
          a(c, b - 10, u, d), a(b + 10, f, u, d);
          return;
        }
      let m = Fu(e, (b) => b.from >= u.from && b.to <= u.to && Math.abs(b.from - c) < r && Math.abs(b.to - f) < r && !g.some((v) => b.from < v && b.to > v));
      if (!m) {
        if (f < u.to && t && i && t.visibleRanges.some((w) => w.from <= f && w.to >= f)) {
          let w = t.moveToLineBoundary(y.cursor(f), !1, !0).head;
          w > c && (f = w);
        }
        let b = this.gapSize(u, c, f, d), v = i || b < 2e6 ? b : 2e6;
        m = new bs(c, f, b, v);
      }
      l.push(m);
    }, h = (c) => {
      if (c.length < o || c.type != re.Text)
        return;
      let f = Vu(c.from, c.to, this.stateDeco);
      if (f.total < o)
        return;
      let u = this.scrollTarget ? this.scrollTarget.range.head : null, d, p;
      if (i) {
        let g = s / this.heightOracle.lineLength * this.heightOracle.lineHeight, m, b;
        if (u != null) {
          let v = rn(f, u), w = ((this.visibleBottom - this.visibleTop) / 2 + g) / c.height;
          m = v - w, b = v + w;
        } else
          m = (this.visibleTop - c.top - g) / c.height, b = (this.visibleBottom - c.top + g) / c.height;
        d = sn(f, m), p = sn(f, b);
      } else {
        let g = f.total * this.heightOracle.charWidth, m = s * this.heightOracle.charWidth, b = 0;
        if (g > 2e6)
          for (let S of e)
            S.from >= c.from && S.from < c.to && S.size != S.displaySize && S.from * this.heightOracle.charWidth + b < this.pixelViewport.left && (b = S.size - S.displaySize);
        let v = this.pixelViewport.left + b, w = this.pixelViewport.right + b, O, k;
        if (u != null) {
          let S = rn(f, u), A = ((w - v) / 2 + m) / g;
          O = S - A, k = S + A;
        } else
          O = (v - m) / g, k = (w + m) / g;
        d = sn(f, O), p = sn(f, k);
      }
      d > c.from && a(c.from, d, c, f), p < c.to && a(p, c.to, c, f);
    };
    for (let c of this.viewportLines)
      Array.isArray(c.type) ? c.type.forEach(h) : h(c);
    return l;
  }
  gapSize(e, t, i, s) {
    let r = rn(s, i) - rn(s, t);
    return this.heightOracle.lineWrapping ? e.height * r : s.total * this.heightOracle.charWidth * r;
  }
  updateLineGaps(e) {
    bs.same(e, this.lineGaps) || (this.lineGaps = e, this.lineGapDeco = K.set(e.map((t) => t.draw(this, this.heightOracle.lineWrapping))));
  }
  computeVisibleRanges(e) {
    let t = this.stateDeco;
    this.lineGaps.length && (t = t.concat(this.lineGapDeco));
    let i = [];
    B.spans(t, this.viewport.from, this.viewport.to, {
      span(r, o) {
        i.push({ from: r, to: o });
      },
      point() {
      }
    }, 20);
    let s = 0;
    if (i.length != this.visibleRanges.length)
      s = 12;
    else
      for (let r = 0; r < i.length && !(s & 8); r++) {
        let o = this.visibleRanges[r], l = i[r];
        (o.from != l.from || o.to != l.to) && (s |= 4, e && e.mapPos(o.from, -1) == l.from && e.mapPos(o.to, 1) == l.to || (s |= 8));
      }
    return this.visibleRanges = i, s;
  }
  lineBlockAt(e) {
    return e >= this.viewport.from && e <= this.viewport.to && this.viewportLines.find((t) => t.from <= e && t.to >= e) || bi(this.heightMap.lineAt(e, j.ByPos, this.heightOracle, 0, 0), this.scaler);
  }
  lineBlockAtHeight(e) {
    return e >= this.viewportLines[0].top && e <= this.viewportLines[this.viewportLines.length - 1].bottom && this.viewportLines.find((t) => t.top <= e && t.bottom >= e) || bi(this.heightMap.lineAt(this.scaler.fromDOM(e), j.ByHeight, this.heightOracle, 0, 0), this.scaler);
  }
  getScrollOffset() {
    return (this.scrollParent == this.view.scrollDOM ? this.scrollParent.scrollTop : (this.scrollParent ? this.scrollParent.getBoundingClientRect().top : 0) - this.view.contentDOM.getBoundingClientRect().top) * this.scaleY;
  }
  scrollAnchorAt(e) {
    let t = this.lineBlockAtHeight(e + 8);
    return t.from >= this.viewport.from || this.viewportLines[0].top - e > 200 ? t : this.viewportLines[0];
  }
  elementAtHeight(e) {
    return bi(this.heightMap.blockAt(this.scaler.fromDOM(e), this.heightOracle, 0, 0), this.scaler);
  }
  get docHeight() {
    return this.scaler.toDOM(this.heightMap.height);
  }
  get contentHeight() {
    return this.docHeight + this.paddingTop + this.paddingBottom;
  }
}
class nn {
  constructor(e, t) {
    this.from = e, this.to = t;
  }
}
function Vu(n, e, t) {
  let i = [], s = n, r = 0;
  return B.spans(t, n, e, {
    span() {
    },
    point(o, l) {
      o > s && (i.push({ from: s, to: o }), r += o - s), s = l;
    }
  }, 20), s < e && (i.push({ from: s, to: e }), r += e - s), { total: r, ranges: i };
}
function sn({ total: n, ranges: e }, t) {
  if (t <= 0)
    return e[0].from;
  if (t >= 1)
    return e[e.length - 1].to;
  let i = Math.floor(n * t);
  for (let s = 0; ; s++) {
    let { from: r, to: o } = e[s], l = o - r;
    if (i <= l)
      return r + i;
    i -= l;
  }
}
function rn(n, e) {
  let t = 0;
  for (let { from: i, to: s } of n.ranges) {
    if (e <= s) {
      t += e - i;
      break;
    }
    t += s - i;
  }
  return t / n.total;
}
function Fu(n, e) {
  for (let t of n)
    if (e(t))
      return t;
}
const qo = {
  toDOM(n) {
    return n;
  },
  fromDOM(n) {
    return n;
  },
  scale: 1,
  eq(n) {
    return n == this;
  }
};
function Ko(n) {
  let e = n.facet(qn).filter((i) => typeof i != "function"), t = n.facet(Br).filter((i) => typeof i != "function");
  return t.length && e.push(B.join(t)), e;
}
class Nr {
  constructor(e, t, i) {
    let s = 0, r = 0, o = 0;
    this.viewports = i.map(({ from: l, to: a }) => {
      let h = t.lineAt(l, j.ByPos, e, 0, 0).top, c = t.lineAt(a, j.ByPos, e, 0, 0).bottom;
      return s += c - h, { from: l, to: a, top: h, bottom: c, domTop: 0, domBottom: 0 };
    }), this.scale = (7e6 - s) / (t.height - s);
    for (let l of this.viewports)
      l.domTop = o + (l.top - r) * this.scale, o = l.domBottom = l.domTop + (l.bottom - l.top), r = l.bottom;
  }
  toDOM(e) {
    for (let t = 0, i = 0, s = 0; ; t++) {
      let r = t < this.viewports.length ? this.viewports[t] : null;
      if (!r || e < r.top)
        return s + (e - i) * this.scale;
      if (e <= r.bottom)
        return r.domTop + (e - r.top);
      i = r.bottom, s = r.domBottom;
    }
  }
  fromDOM(e) {
    for (let t = 0, i = 0, s = 0; ; t++) {
      let r = t < this.viewports.length ? this.viewports[t] : null;
      if (!r || e < r.domTop)
        return i + (e - s) / this.scale;
      if (e <= r.domBottom)
        return r.top + (e - r.domTop);
      i = r.bottom, s = r.domBottom;
    }
  }
  eq(e) {
    return e instanceof Nr ? this.scale == e.scale && this.viewports.length == e.viewports.length && this.viewports.every((t, i) => t.from == e.viewports[i].from && t.to == e.viewports[i].to) : !1;
  }
}
function bi(n, e) {
  if (e.scale == 1)
    return n;
  let t = e.toDOM(n.top), i = e.toDOM(n.bottom);
  return new Re(n.from, n.length, t, i - t, Array.isArray(n._content) ? n._content.map((s) => bi(s, e)) : n._content);
}
const on = /* @__PURE__ */ M.define({ combine: (n) => n.join(" ") }), rr = /* @__PURE__ */ M.define({ combine: (n) => n.indexOf(!0) > -1 }), or = /* @__PURE__ */ dt.newName(), Za = /* @__PURE__ */ dt.newName(), Qa = /* @__PURE__ */ dt.newName(), eh = { "&light": "." + Za, "&dark": "." + Qa };
function lr(n, e, t) {
  return new dt(e, {
    finish(i) {
      return /&/.test(i) ? i.replace(/&\w*/, (s) => {
        if (s == "&")
          return n;
        if (!t || !t[s])
          throw new RangeError(`Unsupported selector: ${s}`);
        return t[s];
      }) : n + " " + i;
    }
  });
}
const Wu = /* @__PURE__ */ lr("." + or, {
  "&": {
    position: "relative !important",
    boxSizing: "border-box",
    "&.cm-focused": {
      // Provide a simple default outline to make sure a focused
      // editor is visually distinct. Can't leave the default behavior
      // because that will apply to the content element, which is
      // inside the scrollable container and doesn't include the
      // gutters. We also can't use an 'auto' outline, since those
      // are, for some reason, drawn behind the element content, which
      // will cause things like the active line background to cover
      // the outline (#297).
      outline: "1px dotted #212121"
    },
    display: "flex !important",
    flexDirection: "column"
  },
  ".cm-scroller": {
    display: "flex !important",
    alignItems: "flex-start !important",
    fontFamily: "monospace",
    lineHeight: 1.4,
    height: "100%",
    overflowX: "auto",
    position: "relative",
    zIndex: 0,
    overflowAnchor: "none"
  },
  ".cm-content": {
    margin: 0,
    flexGrow: 2,
    flexShrink: 0,
    display: "block",
    whiteSpace: "pre",
    wordWrap: "normal",
    // Issue #456
    boxSizing: "border-box",
    minHeight: "100%",
    padding: "4px 0",
    outline: "none",
    "&[contenteditable=true]": {
      WebkitUserModify: "read-write-plaintext-only"
    }
  },
  ".cm-lineWrapping": {
    whiteSpace_fallback: "pre-wrap",
    // For IE
    whiteSpace: "break-spaces",
    wordBreak: "break-word",
    // For Safari, which doesn't support overflow-wrap: anywhere
    overflowWrap: "anywhere",
    flexShrink: 1
  },
  "&light .cm-content": { caretColor: "black" },
  "&dark .cm-content": { caretColor: "white" },
  ".cm-line": {
    display: "block",
    padding: "0 2px 0 6px"
  },
  ".cm-layer": {
    position: "absolute",
    left: 0,
    top: 0,
    contain: "size style",
    "& > *": {
      position: "absolute"
    }
  },
  "&light .cm-selectionBackground": {
    background: "#d9d9d9"
  },
  "&dark .cm-selectionBackground": {
    background: "#222"
  },
  "&light.cm-focused > .cm-scroller > .cm-selectionLayer .cm-selectionBackground": {
    background: "#d7d4f0"
  },
  "&dark.cm-focused > .cm-scroller > .cm-selectionLayer .cm-selectionBackground": {
    background: "#233"
  },
  ".cm-cursorLayer": {
    pointerEvents: "none"
  },
  "&.cm-focused > .cm-scroller > .cm-cursorLayer": {
    animation: "steps(1) cm-blink 1.2s infinite"
  },
  // Two animations defined so that we can switch between them to
  // restart the animation without forcing another style
  // recomputation.
  "@keyframes cm-blink": { "0%": {}, "50%": { opacity: 0 }, "100%": {} },
  "@keyframes cm-blink2": { "0%": {}, "50%": { opacity: 0 }, "100%": {} },
  ".cm-cursor, .cm-dropCursor": {
    borderLeft: "1.2px solid black",
    marginLeft: "-0.6px",
    pointerEvents: "none"
  },
  ".cm-cursor": {
    display: "none"
  },
  "&dark .cm-cursor": {
    borderLeftColor: "#ddd"
  },
  ".cm-selectionHandle": {
    backgroundColor: "currentColor",
    width: "1.5px"
  },
  ".cm-selectionHandle-start::before, .cm-selectionHandle-end::before": {
    content: '""',
    backgroundColor: "inherit",
    borderRadius: "50%",
    width: "8px",
    height: "8px",
    position: "absolute",
    left: "-3.25px"
  },
  ".cm-selectionHandle-start::before": { top: "-8px" },
  ".cm-selectionHandle-end::before": { bottom: "-8px" },
  ".cm-dropCursor": {
    position: "absolute"
  },
  "&.cm-focused > .cm-scroller > .cm-cursorLayer .cm-cursor": {
    display: "block"
  },
  ".cm-iso": {
    unicodeBidi: "isolate"
  },
  ".cm-announced": {
    position: "fixed",
    top: "-10000px"
  },
  "@media print": {
    ".cm-announced": { display: "none" }
  },
  "&light .cm-activeLine": { backgroundColor: "#cceeff44" },
  "&dark .cm-activeLine": { backgroundColor: "#99eeff33" },
  "&light .cm-specialChar": { color: "red" },
  "&dark .cm-specialChar": { color: "#f78" },
  ".cm-gutters": {
    flexShrink: 0,
    display: "flex",
    height: "100%",
    boxSizing: "border-box",
    zIndex: 200
  },
  ".cm-gutters-before": { insetInlineStart: 0 },
  ".cm-gutters-after": { insetInlineEnd: 0 },
  "&light .cm-gutters": {
    backgroundColor: "#f5f5f5",
    color: "#6c6c6c",
    border: "0px solid #ddd",
    "&.cm-gutters-before": { borderRightWidth: "1px" },
    "&.cm-gutters-after": { borderLeftWidth: "1px" }
  },
  "&dark .cm-gutters": {
    backgroundColor: "#333338",
    color: "#ccc"
  },
  ".cm-gutter": {
    display: "flex !important",
    // Necessary -- prevents margin collapsing
    flexDirection: "column",
    flexShrink: 0,
    boxSizing: "border-box",
    minHeight: "100%",
    overflow: "hidden"
  },
  ".cm-gutterElement": {
    boxSizing: "border-box"
  },
  ".cm-lineNumbers .cm-gutterElement": {
    padding: "0 3px 0 5px",
    minWidth: "20px",
    textAlign: "right",
    whiteSpace: "nowrap"
  },
  "&light .cm-activeLineGutter": {
    backgroundColor: "#e2f2ff"
  },
  "&dark .cm-activeLineGutter": {
    backgroundColor: "#222227"
  },
  ".cm-panels": {
    boxSizing: "border-box",
    position: "sticky",
    left: 0,
    right: 0,
    zIndex: 300
  },
  "&light .cm-panels": {
    backgroundColor: "#f5f5f5",
    color: "black"
  },
  "&light .cm-panels-top": {
    borderBottom: "1px solid #ddd"
  },
  "&light .cm-panels-bottom": {
    borderTop: "1px solid #ddd"
  },
  "&dark .cm-panels": {
    backgroundColor: "#333338",
    color: "white"
  },
  ".cm-dialog": {
    padding: "2px 19px 4px 6px",
    position: "relative",
    "& label": { fontSize: "80%" }
  },
  ".cm-dialog-close": {
    position: "absolute",
    top: "3px",
    right: "4px",
    backgroundColor: "inherit",
    border: "none",
    font: "inherit",
    fontSize: "14px",
    padding: "0"
  },
  ".cm-tab": {
    display: "inline-block",
    overflow: "hidden",
    verticalAlign: "bottom"
  },
  ".cm-widgetBuffer": {
    verticalAlign: "text-top",
    height: "1em",
    width: 0,
    display: "inline"
  },
  ".cm-placeholder": {
    color: "#888",
    display: "inline-block",
    verticalAlign: "top",
    userSelect: "none"
  },
  ".cm-highlightSpace": {
    backgroundImage: "radial-gradient(circle at 50% 55%, #aaa 20%, transparent 5%)",
    backgroundPosition: "center"
  },
  ".cm-highlightTab": {
    backgroundImage: `url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="200" height="20"><path stroke="%23888" stroke-width="1" fill="none" d="M1 10H196L190 5M190 15L196 10M197 4L197 16"/></svg>')`,
    backgroundSize: "auto 100%",
    backgroundPosition: "right 90%",
    backgroundRepeat: "no-repeat"
  },
  ".cm-trailingSpace": {
    backgroundColor: "#ff332255"
  },
  ".cm-button": {
    verticalAlign: "middle",
    color: "inherit",
    fontSize: "70%",
    padding: ".2em 1em",
    borderRadius: "1px"
  },
  "&light .cm-button": {
    backgroundImage: "linear-gradient(#eff1f5, #d9d9df)",
    border: "1px solid #888",
    "&:active": {
      backgroundImage: "linear-gradient(#b4b4b4, #d0d3d6)"
    }
  },
  "&dark .cm-button": {
    backgroundImage: "linear-gradient(#393939, #111)",
    border: "1px solid #888",
    "&:active": {
      backgroundImage: "linear-gradient(#111, #333)"
    }
  },
  ".cm-textfield": {
    verticalAlign: "middle",
    color: "inherit",
    fontSize: "70%",
    border: "1px solid silver",
    padding: ".2em .5em"
  },
  "&light .cm-textfield": {
    backgroundColor: "white"
  },
  "&dark .cm-textfield": {
    border: "1px solid #555",
    backgroundColor: "inherit"
  }
}, eh), _u = {
  childList: !0,
  characterData: !0,
  subtree: !0,
  attributes: !0,
  characterDataOldValue: !0
}, ys = C.ie && C.ie_version <= 11;
class zu {
  constructor(e) {
    this.view = e, this.active = !1, this.editContext = null, this.selectionRange = new wf(), this.selectionChanged = !1, this.delayedFlush = -1, this.resizeTimeout = -1, this.queue = [], this.delayedAndroidKey = null, this.flushingAndroidKey = -1, this.lastChange = 0, this.scrollTargets = [], this.intersection = null, this.resizeScroll = null, this.intersecting = !1, this.gapIntersection = null, this.gaps = [], this.printQuery = null, this.parentCheck = -1, this.dom = e.contentDOM, this.observer = new MutationObserver((t) => {
      for (let i of t)
        this.queue.push(i);
      (C.ie && C.ie_version <= 11 || C.ios && e.composing) && t.some((i) => i.type == "childList" && i.removedNodes.length || i.type == "characterData" && i.oldValue.length > i.target.nodeValue.length) ? this.flushSoon() : this.flush();
    }), window.EditContext && C.android && e.constructor.EDIT_CONTEXT !== !1 && // Chrome <126 doesn't support inverted selections in edit context (#1392)
    !(C.chrome && C.chrome_version < 126) && (this.editContext = new ju(e), e.state.facet(et) && (e.contentDOM.editContext = this.editContext.editContext)), ys && (this.onCharData = (t) => {
      this.queue.push({
        target: t.target,
        type: "characterData",
        oldValue: t.prevValue
      }), this.flushSoon();
    }), this.onSelectionChange = this.onSelectionChange.bind(this), this.onResize = this.onResize.bind(this), this.onPrint = this.onPrint.bind(this), this.onScroll = this.onScroll.bind(this), window.matchMedia && (this.printQuery = window.matchMedia("print")), typeof ResizeObserver == "function" && (this.resizeScroll = new ResizeObserver(() => {
      var t;
      ((t = this.view.docView) === null || t === void 0 ? void 0 : t.lastUpdate) < Date.now() - 75 && this.onResize();
    }), this.resizeScroll.observe(e.scrollDOM)), this.addWindowListeners(this.win = e.win), this.start(), typeof IntersectionObserver == "function" && (this.intersection = new IntersectionObserver((t) => {
      this.parentCheck < 0 && (this.parentCheck = setTimeout(this.listenForScroll.bind(this), 1e3)), t.length > 0 && t[t.length - 1].intersectionRatio > 0 != this.intersecting && (this.intersecting = !this.intersecting, this.intersecting != this.view.inView && this.onScrollChanged(document.createEvent("Event")));
    }, { threshold: [0, 1e-3] }), this.intersection.observe(this.dom), this.gapIntersection = new IntersectionObserver((t) => {
      t.length > 0 && t[t.length - 1].intersectionRatio > 0 && this.onScrollChanged(document.createEvent("Event"));
    }, {})), this.listenForScroll(), this.readSelectionRange();
  }
  onScrollChanged(e) {
    this.view.inputState.runHandlers("scroll", e), this.intersecting && this.view.measure();
  }
  onScroll(e) {
    this.intersecting && this.flush(!1), this.editContext && this.view.requestMeasure(this.editContext.measureReq), this.onScrollChanged(e);
  }
  onResize() {
    this.resizeTimeout < 0 && (this.resizeTimeout = setTimeout(() => {
      this.resizeTimeout = -1, this.view.requestMeasure();
    }, 50));
  }
  onPrint(e) {
    (e.type == "change" || !e.type) && !e.matches || (this.view.viewState.printing = !0, this.view.measure(), setTimeout(() => {
      this.view.viewState.printing = !1, this.view.requestMeasure();
    }, 500));
  }
  updateGaps(e) {
    if (this.gapIntersection && (e.length != this.gaps.length || this.gaps.some((t, i) => t != e[i]))) {
      this.gapIntersection.disconnect();
      for (let t of e)
        this.gapIntersection.observe(t);
      this.gaps = e;
    }
  }
  onSelectionChange(e) {
    let t = this.selectionChanged;
    if (!this.readSelectionRange() || this.delayedAndroidKey)
      return;
    let { view: i } = this, s = this.selectionRange;
    if (i.state.facet(et) ? i.root.activeElement != this.dom : !xi(this.dom, s))
      return;
    let r = s.anchorNode && i.docView.tile.nearest(s.anchorNode);
    if (r && r.isWidget() && r.widget.ignoreEvent(e)) {
      t || (this.selectionChanged = !1);
      return;
    }
    (C.ie && C.ie_version <= 11 || C.android && C.chrome) && !i.state.selection.main.empty && // (Selection.isCollapsed isn't reliable on IE)
    s.focusNode && ki(s.focusNode, s.focusOffset, s.anchorNode, s.anchorOffset) ? this.flushSoon() : this.flush(!1);
  }
  readSelectionRange() {
    let { view: e } = this, t = Ri(e.root);
    if (!t)
      return !1;
    let i = C.safari && e.root.nodeType == 11 && e.root.activeElement == this.dom && Uu(this.view, t) || t;
    if (!i || this.selectionRange.eq(i))
      return !1;
    let s = xi(this.dom, i);
    return s && !this.selectionChanged && e.inputState.lastFocusTime > Date.now() - 200 && e.inputState.lastTouchTime < Date.now() - 300 && xf(this.dom, i) ? (this.view.inputState.lastFocusTime = 0, e.docView.updateSelection(), !1) : (this.selectionRange.setRange(i), s && (this.selectionChanged = !0), !0);
  }
  setSelectionRange(e, t) {
    this.selectionRange.set(e.node, e.offset, t.node, t.offset), this.selectionChanged = !1;
  }
  clearSelectionRange() {
    this.selectionRange.set(null, 0, null, 0);
  }
  listenForScroll() {
    this.parentCheck = -1;
    let e = 0, t = null;
    for (let i = this.dom; i; )
      if (i.nodeType == 1)
        !t && e < this.scrollTargets.length && this.scrollTargets[e] == i ? e++ : t || (t = this.scrollTargets.slice(0, e)), t && t.push(i), i = i.assignedSlot || i.parentNode;
      else if (i.nodeType == 11)
        i = i.host;
      else
        break;
    if (e < this.scrollTargets.length && !t && (t = this.scrollTargets.slice(0, e)), t) {
      for (let i of this.scrollTargets)
        i.removeEventListener("scroll", this.onScroll);
      for (let i of this.scrollTargets = t)
        i.addEventListener("scroll", this.onScroll);
    }
  }
  ignore(e) {
    if (!this.active)
      return e();
    try {
      return this.stop(), e();
    } finally {
      this.start(), this.clear();
    }
  }
  start() {
    this.active || (this.observer.observe(this.dom, _u), ys && this.dom.addEventListener("DOMCharacterDataModified", this.onCharData), this.active = !0);
  }
  stop() {
    this.active && (this.active = !1, this.observer.disconnect(), ys && this.dom.removeEventListener("DOMCharacterDataModified", this.onCharData));
  }
  // Throw away any pending changes
  clear() {
    this.processRecords(), this.queue.length = 0, this.selectionChanged = !1;
  }
  // Chrome Android, especially in combination with GBoard, not only
  // doesn't reliably fire regular key events, but also often
  // surrounds the effect of enter or backspace with a bunch of
  // composition events that, when interrupted, cause text duplication
  // or other kinds of corruption. This hack makes the editor back off
  // from handling DOM changes for a moment when such a key is
  // detected (via beforeinput or keydown), and then tries to flush
  // them or, if that has no effect, dispatches the given key.
  delayAndroidKey(e, t) {
    var i;
    if (!this.delayedAndroidKey) {
      let s = () => {
        let r = this.delayedAndroidKey;
        r && (this.clearDelayedAndroidKey(), this.view.inputState.lastKeyCode = r.keyCode, this.view.inputState.lastKeyTime = Date.now(), !this.flush() && r.force && Kt(this.dom, r.key, r.keyCode));
      };
      this.flushingAndroidKey = this.view.win.requestAnimationFrame(s);
    }
    (!this.delayedAndroidKey || e == "Enter") && (this.delayedAndroidKey = {
      key: e,
      keyCode: t,
      // Only run the key handler when no changes are detected if
      // this isn't coming right after another change, in which case
      // it is probably part of a weird chain of updates, and should
      // be ignored if it returns the DOM to its previous state.
      force: this.lastChange < Date.now() - 50 || !!(!((i = this.delayedAndroidKey) === null || i === void 0) && i.force)
    });
  }
  clearDelayedAndroidKey() {
    this.win.cancelAnimationFrame(this.flushingAndroidKey), this.delayedAndroidKey = null, this.flushingAndroidKey = -1;
  }
  flushSoon() {
    this.delayedFlush < 0 && (this.delayedFlush = this.view.win.requestAnimationFrame(() => {
      this.delayedFlush = -1, this.flush();
    }));
  }
  forceFlush() {
    this.delayedFlush >= 0 && (this.view.win.cancelAnimationFrame(this.delayedFlush), this.delayedFlush = -1), this.flush();
  }
  pendingRecords() {
    for (let e of this.observer.takeRecords())
      this.queue.push(e);
    return this.queue;
  }
  processRecords() {
    let e = this.pendingRecords();
    e.length && (this.queue = []);
    let t = -1, i = -1, s = !1;
    for (let r of e) {
      let o = this.readMutation(r);
      o && (o.typeOver && (s = !0), t == -1 ? { from: t, to: i } = o : (t = Math.min(o.from, t), i = Math.max(o.to, i)));
    }
    return { from: t, to: i, typeOver: s };
  }
  readChange() {
    let { from: e, to: t, typeOver: i } = this.processRecords(), s = this.selectionChanged && xi(this.dom, this.selectionRange);
    if (e < 0 && !s)
      return null;
    e > -1 && (this.lastChange = Date.now()), this.view.inputState.lastFocusTime = 0, this.selectionChanged = !1;
    let r = new au(this.view, e, t, i);
    return this.view.docView.domChanged = { newSel: r.newSel ? r.newSel.main : null }, r;
  }
  // Apply pending changes, if any
  flush(e = !0) {
    if (this.delayedFlush >= 0 || this.delayedAndroidKey)
      return !1;
    e && this.readSelectionRange();
    let t = this.readChange();
    if (!t)
      return this.view.requestMeasure(), !1;
    let i = this.view.state, s = Wa(this.view, t);
    return this.view.state == i && (t.domChanged || t.newSel && !Pn(this.view.state.selection, t.newSel.main)) && this.view.update([]), s;
  }
  readMutation(e) {
    let t = this.view.docView.tile.nearest(e.target);
    if (!t || t.isWidget())
      return null;
    if (t.markDirty(e.type == "attributes"), e.type == "childList") {
      let i = Go(t, e.previousSibling || e.target.previousSibling, -1), s = Go(t, e.nextSibling || e.target.nextSibling, 1);
      return {
        from: i ? t.posAfter(i) : t.posAtStart,
        to: s ? t.posBefore(s) : t.posAtEnd,
        typeOver: !1
      };
    } else return e.type == "characterData" ? { from: t.posAtStart, to: t.posAtEnd, typeOver: e.target.nodeValue == e.oldValue } : null;
  }
  setWindow(e) {
    e != this.win && (this.removeWindowListeners(this.win), this.win = e, this.addWindowListeners(this.win));
  }
  addWindowListeners(e) {
    e.addEventListener("resize", this.onResize), this.printQuery ? this.printQuery.addEventListener ? this.printQuery.addEventListener("change", this.onPrint) : this.printQuery.addListener(this.onPrint) : e.addEventListener("beforeprint", this.onPrint), e.addEventListener("scroll", this.onScroll), e.document.addEventListener("selectionchange", this.onSelectionChange);
  }
  removeWindowListeners(e) {
    e.removeEventListener("scroll", this.onScroll), e.removeEventListener("resize", this.onResize), this.printQuery ? this.printQuery.removeEventListener ? this.printQuery.removeEventListener("change", this.onPrint) : this.printQuery.removeListener(this.onPrint) : e.removeEventListener("beforeprint", this.onPrint), e.document.removeEventListener("selectionchange", this.onSelectionChange);
  }
  update(e) {
    this.editContext && (this.editContext.update(e), e.startState.facet(et) != e.state.facet(et) && (e.view.contentDOM.editContext = e.state.facet(et) ? this.editContext.editContext : null));
  }
  destroy() {
    var e, t, i;
    this.stop(), (e = this.intersection) === null || e === void 0 || e.disconnect(), (t = this.gapIntersection) === null || t === void 0 || t.disconnect(), (i = this.resizeScroll) === null || i === void 0 || i.disconnect();
    for (let s of this.scrollTargets)
      s.removeEventListener("scroll", this.onScroll);
    this.removeWindowListeners(this.win), clearTimeout(this.parentCheck), clearTimeout(this.resizeTimeout), this.win.cancelAnimationFrame(this.delayedFlush), this.win.cancelAnimationFrame(this.flushingAndroidKey), this.editContext && (this.view.contentDOM.editContext = null, this.editContext.destroy());
  }
}
function Go(n, e, t) {
  for (; e; ) {
    let i = J.get(e);
    if (i && i.parent == n)
      return i;
    let s = e.parentNode;
    e = s != n.dom ? s : t > 0 ? e.nextSibling : e.previousSibling;
  }
  return null;
}
function Jo(n, e) {
  let t = e.startContainer, i = e.startOffset, s = e.endContainer, r = e.endOffset, o = n.docView.domAtPos(n.state.selection.main.anchor, 1);
  return ki(o.node, o.offset, s, r) && ([t, i, s, r] = [s, r, t, i]), { anchorNode: t, anchorOffset: i, focusNode: s, focusOffset: r };
}
function Uu(n, e) {
  if (e.getComposedRanges) {
    let s = e.getComposedRanges(n.root)[0];
    if (s)
      return Jo(n, s);
  }
  let t = null;
  function i(s) {
    s.preventDefault(), s.stopImmediatePropagation(), t = s.getTargetRanges()[0];
  }
  return n.contentDOM.addEventListener("beforeinput", i, !0), n.dom.ownerDocument.execCommand("indent"), n.contentDOM.removeEventListener("beforeinput", i, !0), t ? Jo(n, t) : null;
}
class ju {
  constructor(e) {
    this.from = 0, this.to = 0, this.pendingContextChange = null, this.handlers = /* @__PURE__ */ Object.create(null), this.composing = null, this.resetRange(e.state);
    let t = this.editContext = new window.EditContext({
      text: e.state.doc.sliceString(this.from, this.to),
      selectionStart: this.toContextPos(Math.max(this.from, Math.min(this.to, e.state.selection.main.anchor))),
      selectionEnd: this.toContextPos(e.state.selection.main.head)
    });
    this.handlers.textupdate = (i) => {
      let s = e.state.selection.main, { anchor: r, head: o } = s, l = this.toEditorPos(i.updateRangeStart), a = this.toEditorPos(i.updateRangeEnd);
      e.inputState.composing >= 0 && !this.composing && (this.composing = { contextBase: i.updateRangeStart, editorBase: l, drifted: !1 });
      let h = a - l > i.text.length;
      l == this.from && r < this.from ? l = r : a == this.to && r > this.to && (a = r);
      let c = _a(e.state.sliceDoc(l, a), i.text, (h ? s.from : s.to) - l, h ? "end" : null);
      if (!c) {
        let u = y.single(this.toEditorPos(i.selectionStart), this.toEditorPos(i.selectionEnd));
        Pn(u, s) || e.dispatch({ selection: u, userEvent: "select" });
        return;
      }
      let f = {
        from: c.from + l,
        to: c.toA + l,
        insert: $.of(i.text.slice(c.from, c.toB).split(`
`))
      };
      if ((C.mac || C.android) && f.from == o - 1 && /^\. ?$/.test(i.text) && e.contentDOM.getAttribute("autocorrect") == "off" && (f = { from: l, to: a, insert: $.of([i.text.replace(".", " ")]) }), this.pendingContextChange = f, !e.state.readOnly) {
        let u = this.to - this.from + (f.to - f.from + f.insert.length);
        Ir(e, f, y.single(this.toEditorPos(i.selectionStart, u), this.toEditorPos(i.selectionEnd, u)));
      }
      this.pendingContextChange && (this.revertPending(e.state), this.setSelection(e.state)), f.from < f.to && !f.insert.length && e.inputState.composing >= 0 && !/[\\p{Alphabetic}\\p{Number}_]/.test(t.text.slice(Math.max(0, i.updateRangeStart - 1), Math.min(t.text.length, i.updateRangeStart + 1))) && this.handlers.compositionend(i);
    }, this.handlers.characterboundsupdate = (i) => {
      let s = [], r = null;
      for (let o = this.toEditorPos(i.rangeStart), l = this.toEditorPos(i.rangeEnd); o < l; o++) {
        let a = e.coordsForChar(o);
        r = a && new DOMRect(a.left, a.top, a.right - a.left, a.bottom - a.top) || r || new DOMRect(), s.push(r);
      }
      t.updateCharacterBounds(i.rangeStart, s);
    }, this.handlers.textformatupdate = (i) => {
      let s = [];
      for (let r of i.getTextFormats()) {
        let o = r.underlineStyle, l = r.underlineThickness;
        if (!/none/i.test(o) && !/none/i.test(l)) {
          let a = this.toEditorPos(r.rangeStart), h = this.toEditorPos(r.rangeEnd);
          if (a < h) {
            let c = `text-decoration: underline ${/^[a-z]/.test(o) ? o + " " : o == "Dashed" ? "dashed " : o == "Squiggle" ? "wavy " : ""}${/thin/i.test(l) ? 1 : 2}px`;
            s.push(K.mark({ attributes: { style: c } }).range(a, h));
          }
        }
      }
      e.dispatch({ effects: Ra.of(K.set(s)) });
    }, this.handlers.compositionstart = () => {
      e.inputState.composing < 0 && (e.inputState.composing = 0, e.inputState.compositionFirstChange = !0);
    }, this.handlers.compositionend = () => {
      if (e.inputState.composing = -1, e.inputState.compositionFirstChange = null, this.composing) {
        let { drifted: i } = this.composing;
        this.composing = null, i && this.reset(e.state);
      }
    };
    for (let i in this.handlers)
      t.addEventListener(i, this.handlers[i]);
    this.measureReq = { read: (i) => {
      this.editContext.updateControlBounds(i.contentDOM.getBoundingClientRect());
      let s = Ri(i.root);
      s && s.rangeCount && this.editContext.updateSelectionBounds(s.getRangeAt(0).getBoundingClientRect());
    } };
  }
  applyEdits(e) {
    let t = 0, i = !1, s = this.pendingContextChange;
    return e.changes.iterChanges((r, o, l, a, h) => {
      if (i)
        return;
      let c = h.length - (o - r);
      if (s && o >= s.to)
        if (s.from == r && s.to == o && s.insert.eq(h)) {
          s = this.pendingContextChange = null, t += c, this.to += c;
          return;
        } else
          s = null, this.revertPending(e.state);
      if (r += t, o += t, o <= this.from)
        this.from += c, this.to += c;
      else if (r < this.to) {
        if (r < this.from || o > this.to || this.to - this.from + h.length > 3e4) {
          i = !0;
          return;
        }
        this.editContext.updateText(this.toContextPos(r), this.toContextPos(o), h.toString()), this.to += c;
      }
      t += c;
    }), s && !i && this.revertPending(e.state), !i;
  }
  update(e) {
    let t = this.pendingContextChange, i = e.startState.selection.main;
    this.composing && (this.composing.drifted || !e.changes.touchesRange(i.from, i.to) && e.transactions.some((s) => !s.isUserEvent("input.type") && s.changes.touchesRange(this.from, this.to))) ? (this.composing.drifted = !0, this.composing.editorBase = e.changes.mapPos(this.composing.editorBase)) : !this.applyEdits(e) || !this.rangeIsValid(e.state) ? (this.pendingContextChange = null, this.reset(e.state)) : (e.docChanged || e.selectionSet || t) && this.setSelection(e.state), (e.geometryChanged || e.docChanged || e.selectionSet) && e.view.requestMeasure(this.measureReq);
  }
  resetRange(e) {
    let { head: t } = e.selection.main;
    this.from = Math.max(
      0,
      t - 1e4
      /* CxVp.Margin */
    ), this.to = Math.min(
      e.doc.length,
      t + 1e4
      /* CxVp.Margin */
    );
  }
  reset(e) {
    this.resetRange(e), this.editContext.updateText(0, this.editContext.text.length, e.doc.sliceString(this.from, this.to)), this.setSelection(e);
  }
  revertPending(e) {
    let t = this.pendingContextChange;
    this.pendingContextChange = null, this.editContext.updateText(this.toContextPos(t.from), this.toContextPos(t.from + t.insert.length), e.doc.sliceString(t.from, t.to));
  }
  setSelection(e) {
    let { main: t } = e.selection, i = this.toContextPos(Math.max(this.from, Math.min(this.to, t.anchor))), s = this.toContextPos(t.head);
    (this.editContext.selectionStart != i || this.editContext.selectionEnd != s) && this.editContext.updateSelection(i, s);
  }
  rangeIsValid(e) {
    let { head: t } = e.selection.main;
    return !(this.from > 0 && t - this.from < 500 || this.to < e.doc.length && this.to - t < 500 || this.to - this.from > 1e4 * 3);
  }
  toEditorPos(e, t = this.to - this.from) {
    e = Math.min(e, t);
    let i = this.composing;
    return i && i.drifted ? i.editorBase + (e - i.contextBase) : e + this.from;
  }
  toContextPos(e) {
    let t = this.composing;
    return t && t.drifted ? t.contextBase + (e - t.editorBase) : e - this.from;
  }
  destroy() {
    for (let e in this.handlers)
      this.editContext.removeEventListener(e, this.handlers[e]);
  }
}
class T {
  /**
  The current editor state.
  */
  get state() {
    return this.viewState.state;
  }
  /**
  To be able to display large documents without consuming too much
  memory or overloading the browser, CodeMirror only draws the
  code that is visible (plus a margin around it) to the DOM. This
  property tells you the extent of the current drawn viewport, in
  document positions.
  */
  get viewport() {
    return this.viewState.viewport;
  }
  /**
  When there are, for example, large collapsed ranges in the
  viewport, its size can be a lot bigger than the actual visible
  content. Thus, if you are doing something like styling the
  content in the viewport, it is preferable to only do so for
  these ranges, which are the subset of the viewport that is
  actually drawn.
  */
  get visibleRanges() {
    return this.viewState.visibleRanges;
  }
  /**
  Returns false when the editor is entirely scrolled out of view
  or otherwise hidden.
  */
  get inView() {
    return this.viewState.inView;
  }
  /**
  Indicates whether the user is currently composing text via
  [IME](https://en.wikipedia.org/wiki/Input_method), and at least
  one change has been made in the current composition.
  */
  get composing() {
    return !!this.inputState && this.inputState.composing > 0;
  }
  /**
  Indicates whether the user is currently in composing state. Note
  that on some platforms, like Android, this will be the case a
  lot, since just putting the cursor on a word starts a
  composition there.
  */
  get compositionStarted() {
    return !!this.inputState && this.inputState.composing >= 0;
  }
  /**
  The document or shadow root that the view lives in.
  */
  get root() {
    return this._root;
  }
  /**
  @internal
  */
  get win() {
    return this.dom.ownerDocument.defaultView || window;
  }
  /**
  Construct a new view. You'll want to either provide a `parent`
  option, or put `view.dom` into your document after creating a
  view, so that the user can see the editor.
  */
  constructor(e = {}) {
    var t;
    this.plugins = [], this.pluginMap = /* @__PURE__ */ new Map(), this.editorAttrs = {}, this.contentAttrs = {}, this.bidiCache = [], this.destroyed = !1, this.updateState = 2, this.measureScheduled = -1, this.measureRequests = [], this.contentDOM = document.createElement("div"), this.scrollDOM = document.createElement("div"), this.scrollDOM.tabIndex = -1, this.scrollDOM.className = "cm-scroller", this.scrollDOM.appendChild(this.contentDOM), this.announceDOM = document.createElement("div"), this.announceDOM.className = "cm-announced", this.announceDOM.setAttribute("aria-live", "polite"), this.dom = document.createElement("div"), this.dom.appendChild(this.announceDOM), this.dom.appendChild(this.scrollDOM), e.parent && e.parent.appendChild(this.dom);
    let { dispatch: i } = e;
    this.dispatchTransactions = e.dispatchTransactions || i && ((s) => s.forEach((r) => i(r, this))) || ((s) => this.update(s)), this.dispatch = this.dispatch.bind(this), this._root = e.root || vf(e.parent) || document, this.viewState = new jo(this, e.state || N.create(e)), e.scrollTo && e.scrollTo.is(Qi) && (this.viewState.scrollTarget = e.scrollTo.value.clip(this.viewState.state)), this.plugins = this.state.facet(Wt).map((s) => new us(s));
    for (let s of this.plugins)
      s.update(this);
    this.observer = new zu(this), this.inputState = new uu(this), this.inputState.ensureHandlers(this.plugins), this.docView = new Ro(this), this.mountStyles(), this.updateAttrs(), this.updateState = 0, this.requestMeasure(), !((t = document.fonts) === null || t === void 0) && t.ready && document.fonts.ready.then(() => {
      this.viewState.mustMeasureContent = "refresh", this.requestMeasure();
    });
  }
  dispatch(...e) {
    let t = e.length == 1 && e[0] instanceof te ? e : e.length == 1 && Array.isArray(e[0]) ? e[0] : [this.state.update(...e)];
    this.dispatchTransactions(t, this);
  }
  /**
  Update the view for the given array of transactions. This will
  update the visible document and selection to match the state
  produced by the transactions, and notify view plugins of the
  change. You should usually call
  [`dispatch`](https://codemirror.net/6/docs/ref/#view.EditorView.dispatch) instead, which uses this
  as a primitive.
  */
  update(e) {
    if (this.updateState != 0)
      throw new Error("Calls to EditorView.update are not allowed while an update is in progress");
    let t = !1, i = !1, s, r = this.state;
    for (let u of e) {
      if (u.startState != r)
        throw new RangeError("Trying to update state with a transaction that doesn't start from the previous state.");
      r = u.state;
    }
    if (this.destroyed) {
      this.viewState.state = r;
      return;
    }
    let o = this.hasFocus, l = 0, a = null;
    e.some((u) => u.annotation(Ga)) ? (this.inputState.notifiedFocused = o, l = 1) : o != this.inputState.notifiedFocused && (this.inputState.notifiedFocused = o, a = Ja(r, o), a || (l = 1));
    let h = this.observer.delayedAndroidKey, c = null;
    if (h ? (this.observer.clearDelayedAndroidKey(), c = this.observer.readChange(), (c && !this.state.doc.eq(r.doc) || !this.state.selection.eq(r.selection)) && (c = null)) : this.observer.clear(), r.facet(N.phrases) != this.state.facet(N.phrases))
      return this.setState(r);
    s = Ln.create(this, r, e), s.flags |= l;
    let f = this.viewState.scrollTarget;
    try {
      this.updateState = 2;
      for (let u of e) {
        if (f && (f = f.map(u.changes)), u.scrollIntoView) {
          let { main: d } = u.state.selection, { x: p, y: g } = this.state.facet(T.cursorScrollMargin);
          f = new Gt(d.empty ? d : y.cursor(d.head, d.head > d.anchor ? -1 : 1), "nearest", "nearest", g, p);
        }
        for (let d of u.effects)
          d.is(Qi) && (f = d.value.clip(this.state));
      }
      this.viewState.update(s, f), this.bidiCache = $n.update(this.bidiCache, s.changes), s.empty || (this.updatePlugins(s), this.inputState.update(s)), t = this.docView.update(s), this.state.facet(mi) != this.styleModules && this.mountStyles(), i = this.updateAttrs(), this.showAnnouncements(e), this.docView.updateSelection(t, e.some((u) => u.isUserEvent("select.pointer")));
    } finally {
      this.updateState = 0;
    }
    if (s.startState.facet(on) != s.state.facet(on) && (this.viewState.mustMeasureContent = !0), (t || i || f || this.viewState.mustEnforceCursorAssoc || this.viewState.mustMeasureContent) && this.requestMeasure(), t && this.docViewUpdate(), !s.empty)
      for (let u of this.state.facet(er))
        try {
          u(s);
        } catch (d) {
          Pe(this.state, d, "update listener");
        }
    (a || c) && Promise.resolve().then(() => {
      a && this.state == a.startState && this.dispatch(a), c && !Wa(this, c) && h.force && Kt(this.contentDOM, h.key, h.keyCode);
    });
  }
  /**
  Reset the view to the given state. (This will cause the entire
  document to be redrawn and all view plugins to be reinitialized,
  so you should probably only use it when the new state isn't
  derived from the old state. Otherwise, use
  [`dispatch`](https://codemirror.net/6/docs/ref/#view.EditorView.dispatch) instead.)
  */
  setState(e) {
    if (this.updateState != 0)
      throw new Error("Calls to EditorView.setState are not allowed while an update is in progress");
    if (this.destroyed) {
      this.viewState.state = e;
      return;
    }
    this.updateState = 2;
    let t = this.hasFocus;
    try {
      for (let i of this.plugins)
        i.destroy(this);
      this.viewState = new jo(this, e), this.plugins = e.facet(Wt).map((i) => new us(i)), this.pluginMap.clear();
      for (let i of this.plugins)
        i.update(this);
      this.docView.destroy(), this.docView = new Ro(this), this.inputState.ensureHandlers(this.plugins), this.mountStyles(), this.updateAttrs(), this.bidiCache = [];
    } finally {
      this.updateState = 0;
    }
    t && this.focus(), this.requestMeasure();
  }
  updatePlugins(e) {
    let t = e.startState.facet(Wt), i = e.state.facet(Wt);
    if (t != i) {
      let s = [];
      for (let r of i) {
        let o = t.indexOf(r);
        if (o < 0)
          s.push(new us(r));
        else {
          let l = this.plugins[o];
          l.mustUpdate = e, s.push(l);
        }
      }
      for (let r of this.plugins)
        r.mustUpdate != e && r.destroy(this);
      this.plugins = s, this.pluginMap.clear();
    } else
      for (let s of this.plugins)
        s.mustUpdate = e;
    for (let s = 0; s < this.plugins.length; s++)
      this.plugins[s].update(this);
    t != i && this.inputState.ensureHandlers(this.plugins);
  }
  docViewUpdate() {
    for (let e of this.plugins) {
      let t = e.value;
      if (t && t.docViewUpdate)
        try {
          t.docViewUpdate(this);
        } catch (i) {
          Pe(this.state, i, "doc view update listener");
        }
    }
  }
  /**
  @internal
  */
  measure(e = !0) {
    if (this.destroyed)
      return;
    if (this.measureScheduled > -1 && this.win.cancelAnimationFrame(this.measureScheduled), this.observer.delayedAndroidKey) {
      this.measureScheduled = -1, this.requestMeasure();
      return;
    }
    this.measureScheduled = 0, e && this.observer.forceFlush();
    let t = null, i = this.viewState.scrollParent, s = this.viewState.getScrollOffset(), { scrollAnchorPos: r, scrollAnchorHeight: o } = this.viewState;
    Math.abs(s - this.viewState.scrollOffset) > 1 && (o = -1), this.viewState.scrollAnchorHeight = -1;
    try {
      for (let l = 0; ; l++) {
        if (o < 0)
          if (ga(i || this.win))
            r = -1, o = this.viewState.heightMap.height;
          else {
            let d = this.viewState.scrollAnchorAt(s);
            r = d.from, o = d.top;
          }
        this.updateState = 1;
        let a = this.viewState.measure();
        if (!a && !this.measureRequests.length && this.viewState.scrollTarget == null)
          break;
        if (l > 5) {
          console.warn(this.measureRequests.length ? "Measure loop restarted more than 5 times" : "Viewport failed to stabilize");
          break;
        }
        let h = [];
        a & 4 || ([this.measureRequests, h] = [h, this.measureRequests]);
        let c = h.map((d) => {
          try {
            return d.read(this);
          } catch (p) {
            return Pe(this.state, p), Yo;
          }
        }), f = Ln.create(this, this.state, []), u = !1;
        f.flags |= a, t ? t.flags |= a : t = f, this.updateState = 2, f.empty || (this.updatePlugins(f), this.inputState.update(f), this.updateAttrs(), u = this.docView.update(f), u && this.docViewUpdate());
        for (let d = 0; d < h.length; d++)
          if (c[d] != Yo)
            try {
              let p = h[d];
              p.write && p.write(c[d], this);
            } catch (p) {
              Pe(this.state, p);
            }
        if (u && this.docView.updateSelection(!0), !f.viewportChanged && this.measureRequests.length == 0) {
          if (this.viewState.editorHeight)
            if (this.viewState.scrollTarget) {
              this.docView.scrollIntoView(this.viewState.scrollTarget), this.viewState.scrollTarget = null, o = -1;
              continue;
            } else {
              let p = ((r < 0 ? this.viewState.heightMap.height : this.viewState.lineBlockAt(r).top) - o) / this.scaleY;
              if ((p > 1 || p < -1) && (i == this.scrollDOM || this.hasFocus || Math.max(this.inputState.lastWheelEvent, this.inputState.lastTouchTime) > Date.now() - 100)) {
                s = s + p, i ? i.scrollTop += p : this.win.scrollBy(0, p), o = -1;
                continue;
              }
            }
          break;
        }
      }
    } finally {
      this.updateState = 0, this.measureScheduled = -1;
    }
    if (t && !t.empty)
      for (let l of this.state.facet(er))
        l(t);
  }
  /**
  Get the CSS classes for the currently active editor themes.
  */
  get themeClasses() {
    return or + " " + (this.state.facet(rr) ? Qa : Za) + " " + this.state.facet(on);
  }
  updateAttrs() {
    let e = Xo(this, Ba, {
      class: "cm-editor" + (this.hasFocus ? " cm-focused " : " ") + this.themeClasses
    }), t = {
      spellcheck: "false",
      autocorrect: "off",
      autocapitalize: "off",
      writingsuggestions: "false",
      translate: "no",
      contenteditable: this.state.facet(et) ? "true" : "false",
      class: "cm-content",
      style: `${C.tabSize}: ${this.state.tabSize}`,
      role: "textbox",
      "aria-multiline": "true"
    };
    this.state.readOnly && (t["aria-readonly"] = "true"), Xo(this, Rr, t);
    let i = this.observer.ignore(() => {
      let s = Mo(this.contentDOM, this.contentAttrs, t), r = Mo(this.dom, this.editorAttrs, e);
      return s || r;
    });
    return this.editorAttrs = e, this.contentAttrs = t, i;
  }
  showAnnouncements(e) {
    let t = !0;
    for (let i of e)
      for (let s of i.effects)
        if (s.is(T.announce)) {
          t && (this.announceDOM.textContent = ""), t = !1;
          let r = this.announceDOM.appendChild(document.createElement("div"));
          r.textContent = s.value;
        }
  }
  mountStyles() {
    this.styleModules = this.state.facet(mi);
    let e = this.state.facet(T.cspNonce);
    dt.mount(this.root, this.styleModules.concat(Wu).reverse(), e ? { nonce: e } : void 0);
  }
  readMeasured() {
    if (this.updateState == 2)
      throw new Error("Reading the editor layout isn't allowed during an update");
    this.updateState == 0 && this.measureScheduled > -1 && this.measure(!1);
  }
  /**
  Schedule a layout measurement, optionally providing callbacks to
  do custom DOM measuring followed by a DOM write phase. Using
  this is preferable reading DOM layout directly from, for
  example, an event handler, because it'll make sure measuring and
  drawing done by other components is synchronized, avoiding
  unnecessary DOM layout computations.
  */
  requestMeasure(e) {
    if (this.measureScheduled < 0 && (this.measureScheduled = this.win.requestAnimationFrame(() => this.measure())), e) {
      if (this.measureRequests.indexOf(e) > -1)
        return;
      if (e.key != null) {
        for (let t = 0; t < this.measureRequests.length; t++)
          if (this.measureRequests[t].key === e.key) {
            this.measureRequests[t] = e;
            return;
          }
      }
      this.measureRequests.push(e);
    }
  }
  /**
  Get the value of a specific plugin, if present. Note that
  plugins that crash can be dropped from a view, so even when you
  know you registered a given plugin, it is recommended to check
  the return value of this method.
  */
  plugin(e) {
    let t = this.pluginMap.get(e);
    return (t === void 0 || t && t.plugin != e) && this.pluginMap.set(e, t = this.plugins.find((i) => i.plugin == e) || null), t && t.update(this).value;
  }
  /**
  The top position of the document, in screen coordinates. This
  may be negative when the editor is scrolled down. Points
  directly to the top of the first line, not above the padding.
  */
  get documentTop() {
    return this.contentDOM.getBoundingClientRect().top + this.viewState.paddingTop;
  }
  /**
  Reports the padding above and below the document.
  */
  get documentPadding() {
    return { top: this.viewState.paddingTop, bottom: this.viewState.paddingBottom };
  }
  /**
  If the editor is transformed with CSS, this provides the scale
  along the X axis. Otherwise, it will just be 1. Note that
  transforms other than translation and scaling are not supported.
  */
  get scaleX() {
    return this.viewState.scaleX;
  }
  /**
  Provide the CSS transformed scale along the Y axis.
  */
  get scaleY() {
    return this.viewState.scaleY;
  }
  /**
  Find the text line or block widget at the given vertical
  position (which is interpreted as relative to the [top of the
  document](https://codemirror.net/6/docs/ref/#view.EditorView.documentTop)).
  */
  elementAtHeight(e) {
    return this.readMeasured(), this.viewState.elementAtHeight(e);
  }
  /**
  Find the line block (see
  [`lineBlockAt`](https://codemirror.net/6/docs/ref/#view.EditorView.lineBlockAt)) at the given
  height, again interpreted relative to the [top of the
  document](https://codemirror.net/6/docs/ref/#view.EditorView.documentTop).
  */
  lineBlockAtHeight(e) {
    return this.readMeasured(), this.viewState.lineBlockAtHeight(e);
  }
  /**
  Get the extent and vertical position of all [line
  blocks](https://codemirror.net/6/docs/ref/#view.EditorView.lineBlockAt) in the viewport. Positions
  are relative to the [top of the
  document](https://codemirror.net/6/docs/ref/#view.EditorView.documentTop);
  */
  get viewportLineBlocks() {
    return this.viewState.viewportLines;
  }
  /**
  Find the line block around the given document position. A line
  block is a range delimited on both sides by either a
  non-[hidden](https://codemirror.net/6/docs/ref/#view.Decoration^replace) line break, or the
  start/end of the document. It will usually just hold a line of
  text, but may be broken into multiple textblocks by block
  widgets.
  */
  lineBlockAt(e) {
    return this.viewState.lineBlockAt(e);
  }
  /**
  The editor's total content height.
  */
  get contentHeight() {
    return this.viewState.contentHeight;
  }
  /**
  Move a cursor position by [grapheme
  cluster](https://codemirror.net/6/docs/ref/#state.findClusterBreak). `forward` determines whether
  the motion is away from the line start, or towards it. In
  bidirectional text, the line is traversed in visual order, using
  the editor's [text direction](https://codemirror.net/6/docs/ref/#view.EditorView.textDirection).
  When the start position was the last one on the line, the
  returned position will be across the line break. If there is no
  further line, the original position is returned.
  
  By default, this method moves over a single cluster. The
  optional `by` argument can be used to move across more. It will
  be called with the first cluster as argument, and should return
  a predicate that determines, for each subsequent cluster,
  whether it should also be moved over.
  */
  moveByChar(e, t, i) {
    return ms(this, e, Bo(this, e, t, i));
  }
  /**
  Move a cursor position across the next group of either
  [letters](https://codemirror.net/6/docs/ref/#state.EditorState.charCategorizer) or non-letter
  non-whitespace characters.
  */
  moveByGroup(e, t) {
    return ms(this, e, Bo(this, e, t, (i) => iu(this, e.head, i)));
  }
  /**
  Get the cursor position visually at the start or end of a line.
  Note that this may differ from the _logical_ position at its
  start or end (which is simply at `line.from`/`line.to`) if text
  at the start or end goes against the line's base text direction.
  */
  visualLineSide(e, t) {
    let i = this.bidiSpans(e), s = this.textDirectionAt(e.from), r = i[t ? i.length - 1 : 0];
    return y.cursor(r.side(t, s) + e.from, r.forward(!t, s) ? 1 : -1);
  }
  /**
  Move to the next line boundary in the given direction. If
  `includeWrap` is true, line wrapping is on, and there is a
  further wrap point on the current line, the wrap point will be
  returned. Otherwise this function will return the start or end
  of the line.
  */
  moveToLineBoundary(e, t, i = !0) {
    return tu(this, e, t, i);
  }
  /**
  Move a cursor position vertically. When `distance` isn't given,
  it defaults to moving to the next line (including wrapped
  lines). Otherwise, `distance` should provide a positive distance
  in pixels.
  
  When `start` has a
  [`goalColumn`](https://codemirror.net/6/docs/ref/#state.SelectionRange.goalColumn), the vertical
  motion will use that as a target horizontal position. Otherwise,
  the cursor's own horizontal position is used. The returned
  cursor will have its goal column set to whichever column was
  used.
  */
  moveVertically(e, t, i) {
    return ms(this, e, nu(this, e, t, i));
  }
  /**
  Find the DOM parent node and offset (child offset if `node` is
  an element, character offset when it is a text node) at the
  given document position.
  
  Note that for positions that aren't currently in
  `visibleRanges`, the resulting DOM position isn't necessarily
  meaningful (it may just point before or after a placeholder
  element).
  */
  domAtPos(e, t = 1) {
    return this.docView.domAtPos(e, t);
  }
  /**
  Find the document position at the given DOM node. Can be useful
  for associating positions with DOM events. Will raise an error
  when `node` isn't part of the editor content.
  */
  posAtDOM(e, t = 0) {
    return this.docView.posFromDOM(e, t);
  }
  posAtCoords(e, t = !0) {
    this.readMeasured();
    let i = nr(this, e, t);
    return i && i.pos;
  }
  posAndSideAtCoords(e, t = !0) {
    return this.readMeasured(), nr(this, e, t);
  }
  /**
  Get the screen coordinates at the given document position.
  `side` determines whether the coordinates are based on the
  element before (-1) or after (1) the position (if no element is
  available on the given side, the method will transparently use
  another strategy to get reasonable coordinates).
  */
  coordsAtPos(e, t = 1) {
    this.readMeasured();
    let i = this.docView.coordsAt(e, t);
    if (!i || i.left == i.right)
      return i;
    let s = this.state.doc.lineAt(e), r = this.bidiSpans(s), o = r[Je.find(r, e - s.from, -1, t)];
    return En(i, o.dir == q.LTR == t > 0);
  }
  /**
  Return the rectangle around a given character. If `pos` does not
  point in front of a character that is in the viewport and
  rendered (i.e. not replaced, not a line break), this will return
  null. For space characters that are a line wrap point, this will
  return the position before the line break.
  */
  coordsForChar(e) {
    return this.readMeasured(), this.docView.coordsForChar(e);
  }
  /**
  The default width of a character in the editor. May not
  accurately reflect the width of all characters (given variable
  width fonts or styling of invididual ranges).
  */
  get defaultCharacterWidth() {
    return this.viewState.heightOracle.charWidth;
  }
  /**
  The default height of a line in the editor. May not be accurate
  for all lines.
  */
  get defaultLineHeight() {
    return this.viewState.heightOracle.lineHeight;
  }
  /**
  The text direction
  ([`direction`](https://developer.mozilla.org/en-US/docs/Web/CSS/direction)
  CSS property) of the editor's content element.
  */
  get textDirection() {
    return this.viewState.defaultTextDirection;
  }
  /**
  Find the text direction of the block at the given position, as
  assigned by CSS. If
  [`perLineTextDirection`](https://codemirror.net/6/docs/ref/#view.EditorView^perLineTextDirection)
  isn't enabled, or the given position is outside of the viewport,
  this will always return the same as
  [`textDirection`](https://codemirror.net/6/docs/ref/#view.EditorView.textDirection). Note that
  this may trigger a DOM layout.
  */
  textDirectionAt(e) {
    return !this.state.facet(Da) || e < this.viewport.from || e > this.viewport.to ? this.textDirection : (this.readMeasured(), this.docView.textDirectionAt(e));
  }
  /**
  Whether this editor [wraps lines](https://codemirror.net/6/docs/ref/#view.EditorView.lineWrapping)
  (as determined by the
  [`white-space`](https://developer.mozilla.org/en-US/docs/Web/CSS/white-space)
  CSS property of its content element).
  */
  get lineWrapping() {
    return this.viewState.heightOracle.lineWrapping;
  }
  /**
  Returns the bidirectional text structure of the given line
  (which should be in the current document) as an array of span
  objects. The order of these spans matches the [text
  direction](https://codemirror.net/6/docs/ref/#view.EditorView.textDirection)—if that is
  left-to-right, the leftmost spans come first, otherwise the
  rightmost spans come first.
  */
  bidiSpans(e) {
    if (e.length > qu)
      return xa(e.length);
    let t = this.textDirectionAt(e.from), i;
    for (let r of this.bidiCache)
      if (r.from == e.from && r.dir == t && (r.fresh || va(r.isolates, i = Do(this, e))))
        return r.order;
    i || (i = Do(this, e));
    let s = Of(e.text, t, i);
    return this.bidiCache.push(new $n(e.from, e.to, t, i, !0, s)), s;
  }
  /**
  Check whether the editor has focus.
  */
  get hasFocus() {
    var e;
    return (this.dom.ownerDocument.hasFocus() || C.safari && ((e = this.inputState) === null || e === void 0 ? void 0 : e.lastContextMenu) > Date.now() - 3e4) && this.root.activeElement == this.contentDOM;
  }
  /**
  Put focus on the editor.
  */
  focus() {
    this.observer.ignore(() => {
      pa(this.contentDOM), this.docView.updateSelection();
    });
  }
  /**
  Update the [root](https://codemirror.net/6/docs/ref/##view.EditorViewConfig.root) in which the editor lives. This is only
  necessary when moving the editor's existing DOM to a new window or shadow root.
  */
  setRoot(e) {
    this._root != e && (this._root = e, this.observer.setWindow((e.nodeType == 9 ? e : e.ownerDocument).defaultView || window), this.mountStyles());
  }
  /**
  Clean up this editor view, removing its element from the
  document, unregistering event handlers, and notifying
  plugins. The view instance can no longer be used after
  calling this.
  */
  destroy() {
    this.root.activeElement == this.contentDOM && this.contentDOM.blur();
    for (let e of this.plugins)
      e.destroy(this);
    this.plugins = [], this.inputState.destroy(), this.docView.destroy(), this.dom.remove(), this.observer.destroy(), this.measureScheduled > -1 && this.win.cancelAnimationFrame(this.measureScheduled), this.destroyed = !0;
  }
  /**
  Returns an effect that can be
  [added](https://codemirror.net/6/docs/ref/#state.TransactionSpec.effects) to a transaction to
  cause it to scroll the given position or range into view.
  */
  static scrollIntoView(e, t = {}) {
    var i, s, r, o;
    return Qi.of(new Gt(typeof e == "number" ? y.cursor(e) : e, (i = t.y) !== null && i !== void 0 ? i : "nearest", (s = t.x) !== null && s !== void 0 ? s : "nearest", (r = t.yMargin) !== null && r !== void 0 ? r : 5, (o = t.xMargin) !== null && o !== void 0 ? o : 5));
  }
  /**
  Return an effect that resets the editor to its current (at the
  time this method was called) scroll position. Note that this
  only affects the editor's own scrollable element, not parents.
  See also
  [`EditorViewConfig.scrollTo`](https://codemirror.net/6/docs/ref/#view.EditorViewConfig.scrollTo).
  
  The effect should be used with a document identical to the one
  it was created for. Failing to do so is not an error, but may
  not scroll to the expected position. You can
  [map](https://codemirror.net/6/docs/ref/#state.StateEffect.map) the effect to account for changes.
  */
  scrollSnapshot() {
    let { scrollTop: e, scrollLeft: t } = this.scrollDOM, i = this.viewState.scrollAnchorAt(e);
    return Qi.of(new Gt(y.cursor(i.from), "start", "start", i.top - e, t, !0));
  }
  /**
  Enable or disable tab-focus mode, which disables key bindings
  for Tab and Shift-Tab, letting the browser's default
  focus-changing behavior go through instead. This is useful to
  prevent trapping keyboard users in your editor.
  
  Without argument, this toggles the mode. With a boolean, it
  enables (true) or disables it (false). Given a number, it
  temporarily enables the mode until that number of milliseconds
  have passed or another non-Tab key is pressed.
  */
  setTabFocusMode(e) {
    e == null ? this.inputState.tabFocusMode = this.inputState.tabFocusMode < 0 ? 0 : -1 : typeof e == "boolean" ? this.inputState.tabFocusMode = e ? 0 : -1 : this.inputState.tabFocusMode != 0 && (this.inputState.tabFocusMode = Date.now() + e);
  }
  /**
  Returns an extension that can be used to add DOM event handlers.
  The value should be an object mapping event names to handler
  functions. For any given event, such functions are ordered by
  extension precedence, and the first handler to return true will
  be assumed to have handled that event, and no other handlers or
  built-in behavior will be activated for it. These are registered
  on the [content element](https://codemirror.net/6/docs/ref/#view.EditorView.contentDOM), except
  for `scroll` handlers, which will be called any time the
  editor's [scroll element](https://codemirror.net/6/docs/ref/#view.EditorView.scrollDOM) or one of
  its parent nodes is scrolled.
  */
  static domEventHandlers(e) {
    return be.define(() => ({}), { eventHandlers: e });
  }
  /**
  Create an extension that registers DOM event observers. Contrary
  to event [handlers](https://codemirror.net/6/docs/ref/#view.EditorView^domEventHandlers),
  observers can't be prevented from running by a higher-precedence
  handler returning true. They also don't prevent other handlers
  and observers from running when they return true, and should not
  call `preventDefault`.
  */
  static domEventObservers(e) {
    return be.define(() => ({}), { eventObservers: e });
  }
  /**
  Create a theme extension. The first argument can be a
  [`style-mod`](https://code.haverbeke.berlin/marijn/style-mod#documentation)
  style spec providing the styles for the theme. These will be
  prefixed with a generated class for the style.
  
  Because the selectors will be prefixed with a scope class, rule
  that directly match the editor's [wrapper
  element](https://codemirror.net/6/docs/ref/#view.EditorView.dom)—to which the scope class will be
  added—need to be explicitly differentiated by adding an `&` to
  the selector for that element—for example
  `&.cm-focused`.
  
  When `dark` is set to true, the theme will be marked as dark,
  which will cause the `&dark` rules from [base
  themes](https://codemirror.net/6/docs/ref/#view.EditorView^baseTheme) to be used (as opposed to
  `&light` when a light theme is active).
  */
  static theme(e, t) {
    let i = dt.newName(), s = [on.of(i), mi.of(lr(`.${i}`, e))];
    return t && t.dark && s.push(rr.of(!0)), s;
  }
  /**
  Create an extension that adds styles to the base theme. Like
  with [`theme`](https://codemirror.net/6/docs/ref/#view.EditorView^theme), use `&` to indicate the
  place of the editor wrapper element when directly targeting
  that. You can also use `&dark` or `&light` instead to only
  target editors with a dark or light theme.
  */
  static baseTheme(e) {
    return zn.lowest(mi.of(lr("." + or, e, eh)));
  }
  /**
  Retrieve an editor view instance from the view's DOM
  representation.
  */
  static findFromDOM(e) {
    var t;
    let i = e.querySelector(".cm-content"), s = i && J.get(i) || J.get(e);
    return ((t = s?.root) === null || t === void 0 ? void 0 : t.view) || null;
  }
}
T.styleModule = mi;
T.inputHandler = Ta;
T.clipboardInputFilter = Er;
T.clipboardOutputFilter = Lr;
T.scrollHandler = La;
T.focusChangeEffect = Oa;
T.perLineTextDirection = Da;
T.exceptionSink = Ma;
T.updateListener = er;
T.editable = et;
T.mouseSelectionStyle = Ca;
T.dragMovesSelection = Aa;
T.clickAddsSelectionRange = Sa;
T.decorations = qn;
T.blockWrappers = Pa;
T.outerDecorations = Br;
T.atomicRanges = qi;
T.bidiIsolatedRanges = Ia;
T.cursorScrollMargin = /* @__PURE__ */ M.define({
  combine: (n) => {
    let e = 5, t = 5;
    for (let i of n)
      typeof i == "number" ? e = t = i : { x: e, y: t } = i;
    return { x: e, y: t };
  }
});
T.scrollMargins = $a;
T.darkTheme = rr;
T.cspNonce = /* @__PURE__ */ M.define({ combine: (n) => n.length ? n[0] : "" });
T.contentAttributes = Rr;
T.editorAttributes = Ba;
T.lineWrapping = /* @__PURE__ */ T.contentAttributes.of({ class: "cm-lineWrapping" });
T.announce = /* @__PURE__ */ W.define();
const qu = 4096, Yo = {};
class $n {
  constructor(e, t, i, s, r, o) {
    this.from = e, this.to = t, this.dir = i, this.isolates = s, this.fresh = r, this.order = o;
  }
  static update(e, t) {
    if (t.empty && !e.some((r) => r.fresh))
      return e;
    let i = [], s = e.length ? e[e.length - 1].dir : q.LTR;
    for (let r = Math.max(0, e.length - 10); r < e.length; r++) {
      let o = e[r];
      o.dir == s && !t.touchesRange(o.from, o.to) && i.push(new $n(t.mapPos(o.from, 1), t.mapPos(o.to, -1), o.dir, o.isolates, !1, o.order));
    }
    return i;
  }
}
function Xo(n, e, t) {
  for (let i = n.state.facet(e), s = i.length - 1; s >= 0; s--) {
    let r = i[s], o = typeof r == "function" ? r(n) : r;
    o && Tr(o, t);
  }
  return t;
}
const Ku = C.mac ? "mac" : C.windows ? "win" : C.linux ? "linux" : "key";
function Gu(n, e) {
  const t = n.split(/-(?!$)/);
  let i = t[t.length - 1];
  i == "Space" && (i = " ");
  let s, r, o, l;
  for (let a = 0; a < t.length - 1; ++a) {
    const h = t[a];
    if (/^(cmd|meta|m)$/i.test(h))
      l = !0;
    else if (/^a(lt)?$/i.test(h))
      s = !0;
    else if (/^(c|ctrl|control)$/i.test(h))
      r = !0;
    else if (/^s(hift)?$/i.test(h))
      o = !0;
    else if (/^mod$/i.test(h))
      e == "mac" ? l = !0 : r = !0;
    else
      throw new Error("Unrecognized modifier name: " + h);
  }
  return s && (i = "Alt-" + i), r && (i = "Ctrl-" + i), l && (i = "Meta-" + i), o && (i = "Shift-" + i), i;
}
function ln(n, e, t) {
  return e.altKey && (n = "Alt-" + n), e.ctrlKey && (n = "Ctrl-" + n), e.metaKey && (n = "Meta-" + n), t !== !1 && e.shiftKey && (n = "Shift-" + n), n;
}
const Ju = /* @__PURE__ */ zn.default(/* @__PURE__ */ T.domEventHandlers({
  keydown(n, e) {
    return Qu(Yu(e.state), n, e, "editor");
  }
})), th = /* @__PURE__ */ M.define({ enables: Ju }), Zo = /* @__PURE__ */ new WeakMap();
function Yu(n) {
  let e = n.facet(th), t = Zo.get(e);
  return t || Zo.set(e, t = Zu(e.reduce((i, s) => i.concat(s), []))), t;
}
let ht = null;
const Xu = 4e3;
function Zu(n, e = Ku) {
  let t = /* @__PURE__ */ Object.create(null), i = /* @__PURE__ */ Object.create(null), s = (o, l) => {
    let a = i[o];
    if (a == null)
      i[o] = l;
    else if (a != l)
      throw new Error("Key binding " + o + " is used both as a regular binding and as a multi-stroke prefix");
  }, r = (o, l, a, h, c) => {
    var f, u;
    let d = t[o] || (t[o] = /* @__PURE__ */ Object.create(null)), p = l.split(/ (?!$)/).map((b) => Gu(b, e));
    for (let b = 1; b < p.length; b++) {
      let v = p.slice(0, b).join(" ");
      s(v, !0), d[v] || (d[v] = {
        preventDefault: !0,
        stopPropagation: !1,
        run: [(w) => {
          let O = ht = { view: w, prefix: v, scope: o };
          return setTimeout(() => {
            ht == O && (ht = null);
          }, Xu), !0;
        }]
      });
    }
    let g = p.join(" ");
    s(g, !1);
    let m = d[g] || (d[g] = {
      preventDefault: !1,
      stopPropagation: !1,
      run: ((u = (f = d._any) === null || f === void 0 ? void 0 : f.run) === null || u === void 0 ? void 0 : u.slice()) || []
    });
    a && m.run.push(a), h && (m.preventDefault = !0), c && (m.stopPropagation = !0);
  };
  for (let o of n) {
    let l = o.scope ? o.scope.split(" ") : ["editor"];
    if (o.any)
      for (let h of l) {
        let c = t[h] || (t[h] = /* @__PURE__ */ Object.create(null));
        c._any || (c._any = { preventDefault: !1, stopPropagation: !1, run: [] });
        let { any: f } = o;
        for (let u in c)
          c[u].run.push((d) => f(d, ar));
      }
    let a = o[e] || o.key;
    if (a)
      for (let h of l)
        r(h, a, o.run, o.preventDefault, o.stopPropagation), o.shift && r(h, "Shift-" + a, o.shift, o.preventDefault, o.stopPropagation);
  }
  return t;
}
let ar = null;
function Qu(n, e, t, i) {
  ar = e;
  let s = df(e), r = Gc(s, 0), o = Jc(r) == s.length && s != " ", l = "", a = !1, h = !1, c = !1;
  ht && ht.view == t && ht.scope == i && (l = ht.prefix + " ", Ua.indexOf(e.keyCode) < 0 && (h = !0, ht = null));
  let f = /* @__PURE__ */ new Set(), u = (m) => {
    if (m) {
      for (let b of m.run)
        if (!f.has(b) && (f.add(b), b(t)))
          return m.stopPropagation && (c = !0), !0;
      m.preventDefault && (m.stopPropagation && (c = !0), h = !0);
    }
    return !1;
  }, d = n[i], p, g;
  return d && (u(d[l + ln(s, e, !o)]) ? a = !0 : o && (e.altKey || e.metaKey || e.ctrlKey) && // Ctrl-Alt may be used for AltGr on Windows
  !(C.windows && e.ctrlKey && e.altKey) && // Alt-combinations on macOS tend to be typed characters
  !(C.mac && e.altKey && !(e.ctrlKey || e.metaKey)) && (p = pt[e.keyCode]) && p != s ? (u(d[l + ln(p, e, !0)]) || e.shiftKey && (g = Ei[e.keyCode]) != s && g != p && u(d[l + ln(g, e, !1)])) && (a = !0) : o && e.shiftKey && u(d[l + ln(s, e, !0)]) && (a = !0), !a && u(d._any) && (a = !0)), h && (a = !0), a && c && e.stopPropagation(), ar = null, a;
}
class Dt {
  /**
  Create a marker with the given class and dimensions. If `width`
  is null, the DOM element will get no width style.
  */
  constructor(e, t, i, s, r) {
    this.className = e, this.left = t, this.top = i, this.width = s, this.height = r;
  }
  draw() {
    let e = document.createElement("div");
    return e.className = this.className, this.adjust(e), e;
  }
  update(e, t) {
    return t.className != this.className ? !1 : (this.adjust(e), !0);
  }
  adjust(e) {
    e.style.left = this.left + "px", e.style.top = this.top + "px", this.width != null && (e.style.width = this.width + "px"), e.style.height = this.height + "px";
  }
  eq(e) {
    return this.left == e.left && this.top == e.top && this.width == e.width && this.height == e.height && this.className == e.className;
  }
  /**
  Create a set of rectangles for the given selection range,
  assigning them theclass`className`. Will create a single
  rectangle for empty ranges, and a set of selection-style
  rectangles covering the range's content (in a bidi-aware
  way) for non-empty ones.
  */
  static forRange(e, t, i) {
    if (i.empty) {
      let s = e.coordsAtPos(i.head, i.assoc || 1);
      if (!s)
        return [];
      let r = ih(e);
      return [new Dt(t, s.left - r.left, s.top - r.top, null, s.bottom - s.top)];
    } else
      return ed(e, t, i);
  }
}
function ih(n) {
  let e = n.scrollDOM.getBoundingClientRect();
  return { left: (n.textDirection == q.LTR ? e.left : e.right - n.scrollDOM.clientWidth * n.scaleX) - n.scrollDOM.scrollLeft * n.scaleX, top: e.top - n.scrollDOM.scrollTop * n.scaleY };
}
function Qo(n, e, t, i) {
  let s = n.coordsAtPos(e, t * 2);
  if (!s)
    return i;
  let r = n.dom.getBoundingClientRect(), o = (s.top + s.bottom) / 2, l = n.posAtCoords({ x: r.left + 1, y: o }), a = n.posAtCoords({ x: r.right - 1, y: o });
  return l == null || a == null ? i : { from: Math.max(i.from, Math.min(l, a)), to: Math.min(i.to, Math.max(l, a)) };
}
function ed(n, e, t) {
  if (t.to <= n.viewport.from || t.from >= n.viewport.to)
    return [];
  let i = Math.max(t.from, n.viewport.from), s = Math.min(t.to, n.viewport.to), r = n.textDirection == q.LTR, o = n.contentDOM, l = o.getBoundingClientRect(), a = ih(n), h = o.querySelector(".cm-line"), c = h && window.getComputedStyle(h), f = l.left + (c ? parseInt(c.paddingLeft) + Math.min(0, parseInt(c.textIndent)) : 0), u = l.right - (c ? parseInt(c.paddingRight) : 0), d = ir(n, i, 1), p = ir(n, s, -1), g = d.type == re.Text ? d : null, m = p.type == re.Text ? p : null;
  if (g && (n.lineWrapping || d.widgetLineBreaks) && (g = Qo(n, i, 1, g)), m && (n.lineWrapping || p.widgetLineBreaks) && (m = Qo(n, s, -1, m)), g && m && g.from == m.from && g.to == m.to)
    return v(w(t.from, t.to, g));
  {
    let k = g ? w(t.from, null, g) : O(d, !1), S = m ? w(null, t.to, m) : O(p, !0), A = [];
    return (g || d).to < (m || p).from - (g && m ? 1 : 0) || d.widgetLineBreaks > 1 && k.bottom + n.defaultLineHeight / 2 < S.top ? A.push(b(f, k.bottom, u, S.top)) : k.bottom < S.top && n.elementAtHeight((k.bottom + S.top) / 2).type == re.Text && (k.bottom = S.top = (k.bottom + S.top) / 2), v(k).concat(A).concat(v(S));
  }
  function b(k, S, A, L) {
    return new Dt(e, k - a.left, S - a.top, Math.max(0, A - k), L - S);
  }
  function v({ top: k, bottom: S, horizontal: A }) {
    let L = [];
    for (let P = 0; P < A.length; P += 2)
      L.push(b(A[P], k, A[P + 1], S));
    return L;
  }
  function w(k, S, A) {
    let L = 1e9, P = -1e9, _ = [];
    function R(H, U, ce, xe, Fe) {
      let ie = n.coordsAtPos(H, H == A.to ? -2 : 2), Ce = n.coordsAtPos(ce, ce == A.from ? 2 : -2);
      !ie || !Ce || (L = Math.min(ie.top, Ce.top, L), P = Math.max(ie.bottom, Ce.bottom, P), Fe == q.LTR ? _.push(r && U ? f : ie.left, r && xe ? u : Ce.right) : _.push(!r && xe ? f : Ce.left, !r && U ? u : ie.right));
    }
    let D = k ?? A.from, F = S ?? A.to;
    for (let H of n.visibleRanges)
      if (H.to > D && H.from < F)
        for (let U = Math.max(H.from, D), ce = Math.min(H.to, F); ; ) {
          let xe = n.state.doc.lineAt(U);
          for (let Fe of n.bidiSpans(xe)) {
            let ie = Fe.from + xe.from, Ce = Fe.to + xe.from;
            if (ie >= ce)
              break;
            Ce > U && R(Math.max(ie, U), k == null && ie <= D, Math.min(Ce, ce), S == null && Ce >= F, Fe.dir);
          }
          if (U = xe.to + 1, U >= ce)
            break;
        }
    return _.length == 0 && R(D, k == null, F, S == null, n.textDirection), { top: L, bottom: P, horizontal: _ };
  }
  function O(k, S) {
    let A = l.top + (S ? k.top : k.bottom);
    return { top: A, bottom: A, horizontal: [] };
  }
}
function td(n, e) {
  return n.constructor == e.constructor && n.eq(e);
}
class id {
  constructor(e, t) {
    this.view = e, this.layer = t, this.drawn = [], this.scaleX = 1, this.scaleY = 1, this.measureReq = { read: this.measure.bind(this), write: this.draw.bind(this) }, this.dom = e.scrollDOM.appendChild(document.createElement("div")), this.dom.classList.add("cm-layer"), t.above && this.dom.classList.add("cm-layer-above"), t.class && this.dom.classList.add(t.class), this.scale(), this.dom.setAttribute("aria-hidden", "true"), this.setOrder(e.state), e.requestMeasure(this.measureReq), t.mount && t.mount(this.dom, e);
  }
  update(e) {
    e.startState.facet(xn) != e.state.facet(xn) && this.setOrder(e.state), (this.layer.update(e, this.dom) || e.geometryChanged) && (this.scale(), e.view.requestMeasure(this.measureReq));
  }
  docViewUpdate(e) {
    this.layer.updateOnDocViewUpdate !== !1 && e.requestMeasure(this.measureReq);
  }
  setOrder(e) {
    let t = 0, i = e.facet(xn);
    for (; t < i.length && i[t] != this.layer; )
      t++;
    this.dom.style.zIndex = String((this.layer.above ? 150 : -1) - t);
  }
  measure() {
    return this.layer.markers(this.view);
  }
  scale() {
    let { scaleX: e, scaleY: t } = this.view;
    (e != this.scaleX || t != this.scaleY) && (this.scaleX = e, this.scaleY = t, this.dom.style.transform = `scale(${1 / e}, ${1 / t})`);
  }
  draw(e) {
    if (e.length != this.drawn.length || e.some((t, i) => !td(t, this.drawn[i]))) {
      let t = this.dom.firstChild, i = 0;
      for (let s of e)
        s.update && t && s.constructor && this.drawn[i].constructor && s.update(t, this.drawn[i]) ? (t = t.nextSibling, i++) : this.dom.insertBefore(s.draw(), t);
      for (; t; ) {
        let s = t.nextSibling;
        t.remove(), t = s;
      }
      this.drawn = e, C.webkit && (this.dom.style.display = this.dom.firstChild ? "" : "none");
    }
  }
  destroy() {
    this.layer.destroy && this.layer.destroy(this.dom, this.view), this.dom.remove();
  }
}
const xn = /* @__PURE__ */ M.define();
function nh(n) {
  return [
    be.define((e) => new id(e, n)),
    xn.of(n)
  ];
}
const ti = /* @__PURE__ */ M.define({
  combine(n) {
    return _i(n, {
      cursorBlinkRate: 1200,
      drawRangeCursor: !0,
      iosSelectionHandles: !0
    }, {
      cursorBlinkRate: (e, t) => Math.min(e, t),
      drawRangeCursor: (e, t) => e || t
    });
  }
});
function nd(n = {}) {
  return [
    ti.of(n),
    sd,
    rd,
    od,
    Ea.of(!0)
  ];
}
function sh(n) {
  return n.startState.facet(ti) != n.state.facet(ti);
}
const sd = /* @__PURE__ */ nh({
  above: !0,
  markers(n) {
    let { state: e } = n, t = e.facet(ti), i = [];
    for (let s of e.selection.ranges) {
      let r = s == e.selection.main;
      if (s.empty || t.drawRangeCursor && !(r && C.ios && t.iosSelectionHandles)) {
        let o = r ? "cm-cursor cm-cursor-primary" : "cm-cursor cm-cursor-secondary", l = s.empty ? s : y.cursor(s.head, s.assoc);
        for (let a of Dt.forRange(n, o, l))
          i.push(a);
      }
    }
    return i;
  },
  update(n, e) {
    n.transactions.some((i) => i.selection) && (e.style.animationName = e.style.animationName == "cm-blink" ? "cm-blink2" : "cm-blink");
    let t = sh(n);
    return t && el(n.state, e), n.docChanged || n.selectionSet || t;
  },
  mount(n, e) {
    el(e.state, n);
  },
  class: "cm-cursorLayer"
});
function el(n, e) {
  e.style.animationDuration = n.facet(ti).cursorBlinkRate + "ms";
}
const rd = /* @__PURE__ */ nh({
  above: !1,
  markers(n) {
    let e = [], { main: t, ranges: i } = n.state.selection;
    for (let s of i)
      if (!s.empty)
        for (let r of Dt.forRange(n, "cm-selectionBackground", s))
          e.push(r);
    if (C.ios && !t.empty && n.state.facet(ti).iosSelectionHandles) {
      for (let s of Dt.forRange(n, "cm-selectionHandle cm-selectionHandle-start", y.cursor(t.from, 1)))
        e.push(s);
      for (let s of Dt.forRange(n, "cm-selectionHandle cm-selectionHandle-end", y.cursor(t.to, 1)))
        e.push(s);
    }
    return e;
  },
  update(n, e) {
    return n.docChanged || n.selectionSet || n.viewportChanged || sh(n);
  },
  class: "cm-selectionLayer"
}), od = /* @__PURE__ */ zn.highest(/* @__PURE__ */ T.theme({
  ".cm-line": {
    "& ::selection, &::selection": { backgroundColor: "transparent !important" },
    caretColor: "transparent !important"
  },
  ".cm-content": {
    caretColor: "transparent !important",
    "& :focus": {
      caretColor: "initial !important",
      "&::selection, & ::selection": {
        backgroundColor: "Highlight !important"
      }
    }
  }
})), rh = /* @__PURE__ */ W.define({
  map(n, e) {
    return n == null ? null : e.mapPos(n);
  }
}), yi = /* @__PURE__ */ Ae.define({
  create() {
    return null;
  },
  update(n, e) {
    return n != null && (n = e.changes.mapPos(n)), e.effects.reduce((t, i) => i.is(rh) ? i.value : t, n);
  }
}), ld = /* @__PURE__ */ be.fromClass(class {
  constructor(n) {
    this.view = n, this.cursor = null, this.measureReq = { read: this.readPos.bind(this), write: this.drawCursor.bind(this) };
  }
  update(n) {
    var e;
    let t = n.state.field(yi);
    t == null ? this.cursor != null && ((e = this.cursor) === null || e === void 0 || e.remove(), this.cursor = null) : (this.cursor || (this.cursor = this.view.scrollDOM.appendChild(document.createElement("div")), this.cursor.className = "cm-dropCursor"), (n.startState.field(yi) != t || n.docChanged || n.geometryChanged) && this.view.requestMeasure(this.measureReq));
  }
  readPos() {
    let { view: n } = this, e = n.state.field(yi), t = e != null && n.coordsAtPos(e);
    if (!t)
      return null;
    let i = n.scrollDOM.getBoundingClientRect();
    return {
      left: t.left - i.left + n.scrollDOM.scrollLeft * n.scaleX,
      top: t.top - i.top + n.scrollDOM.scrollTop * n.scaleY,
      height: t.bottom - t.top
    };
  }
  drawCursor(n) {
    if (this.cursor) {
      let { scaleX: e, scaleY: t } = this.view;
      n ? (this.cursor.style.left = n.left / e + "px", this.cursor.style.top = n.top / t + "px", this.cursor.style.height = n.height / t + "px") : this.cursor.style.left = "-100000px";
    }
  }
  destroy() {
    this.cursor && this.cursor.remove();
  }
  setDropPos(n) {
    this.view.state.field(yi) != n && this.view.dispatch({ effects: rh.of(n) });
  }
}, {
  eventObservers: {
    dragover(n) {
      this.setDropPos(this.view.posAtCoords({ x: n.clientX, y: n.clientY }));
    },
    dragleave(n) {
      (n.target == this.view.contentDOM || !this.view.contentDOM.contains(n.relatedTarget)) && this.setDropPos(null);
    },
    dragend() {
      this.setDropPos(null);
    },
    drop() {
      this.setDropPos(null);
    }
  }
});
function ad() {
  return [yi, ld];
}
function hd() {
  return fd;
}
const cd = /* @__PURE__ */ K.line({ class: "cm-activeLine" }), fd = /* @__PURE__ */ be.fromClass(class {
  constructor(n) {
    this.decorations = this.getDeco(n);
  }
  update(n) {
    (n.docChanged || n.selectionSet) && (this.decorations = this.getDeco(n.view));
  }
  getDeco(n) {
    let e = -1, t = [];
    for (let i of n.state.selection.ranges) {
      let s = n.lineBlockAt(i.head);
      s.from > e && (t.push(cd.range(s.from)), e = s.from);
    }
    return K.set(t);
  }
}, {
  decorations: (n) => n.decorations
}), an = "-10000px";
class oh {
  constructor(e, t, i, s) {
    this.facet = t, this.createTooltipView = i, this.removeTooltipView = s, this.input = e.state.facet(t), this.tooltips = this.input.filter((o) => o);
    let r = null;
    this.tooltipViews = this.tooltips.map((o) => r = i(o, r));
  }
  update(e, t) {
    var i;
    let s = e.state.facet(this.facet), r = s.filter((a) => a);
    if (s === this.input) {
      for (let a of this.tooltipViews)
        a.update && a.update(e);
      return !1;
    }
    let o = [], l = t ? [] : null;
    for (let a = 0; a < r.length; a++) {
      let h = r[a], c = -1;
      if (h) {
        for (let f = 0; f < this.tooltips.length; f++) {
          let u = this.tooltips[f];
          u && u.create == h.create && (c = f);
        }
        if (c < 0)
          o[a] = this.createTooltipView(h, a ? o[a - 1] : null), l && (l[a] = !!h.above);
        else {
          let f = o[a] = this.tooltipViews[c];
          l && (l[a] = t[c]), f.update && f.update(e);
        }
      }
    }
    for (let a of this.tooltipViews)
      o.indexOf(a) < 0 && (this.removeTooltipView(a), (i = a.destroy) === null || i === void 0 || i.call(a));
    return t && (l.forEach((a, h) => t[h] = a), t.length = l.length), this.input = s, this.tooltips = r, this.tooltipViews = o, !0;
  }
}
function ud(n) {
  let e = n.dom.ownerDocument.documentElement;
  return { top: 0, left: 0, bottom: e.clientHeight, right: e.clientWidth };
}
const ws = /* @__PURE__ */ M.define({
  combine: (n) => {
    var e, t, i;
    return {
      position: C.ios ? "absolute" : ((e = n.find((s) => s.position)) === null || e === void 0 ? void 0 : e.position) || "fixed",
      parent: ((t = n.find((s) => s.parent)) === null || t === void 0 ? void 0 : t.parent) || null,
      tooltipSpace: ((i = n.find((s) => s.tooltipSpace)) === null || i === void 0 ? void 0 : i.tooltipSpace) || ud
    };
  }
}), tl = /* @__PURE__ */ new WeakMap(), lh = /* @__PURE__ */ be.fromClass(class {
  constructor(n) {
    this.view = n, this.above = [], this.inView = !0, this.madeAbsolute = !1, this.lastTransaction = 0, this.measureTimeout = -1;
    let e = n.state.facet(ws);
    this.position = e.position, this.parent = e.parent, this.classes = n.themeClasses, this.createContainer(), this.measureReq = { read: this.readMeasure.bind(this), write: this.writeMeasure.bind(this), key: this }, this.resizeObserver = typeof ResizeObserver == "function" ? new ResizeObserver(() => this.measureSoon()) : null, this.manager = new oh(n, Hr, (t, i) => this.createTooltip(t, i), (t) => {
      this.resizeObserver && this.resizeObserver.unobserve(t.dom), t.dom.remove();
    }), this.above = this.manager.tooltips.map((t) => !!t.above), this.intersectionObserver = typeof IntersectionObserver == "function" ? new IntersectionObserver((t) => {
      Date.now() > this.lastTransaction - 50 && t.length > 0 && t[t.length - 1].intersectionRatio < 1 && this.measureSoon();
    }, { threshold: [1] }) : null, this.observeIntersection(), n.win.addEventListener("resize", this.measureSoon = this.measureSoon.bind(this)), this.maybeMeasure();
  }
  createContainer() {
    this.parent ? (this.container = document.createElement("div"), this.container.style.position = "relative", this.container.className = this.view.themeClasses, this.parent.appendChild(this.container)) : this.container = this.view.dom;
  }
  observeIntersection() {
    if (this.intersectionObserver) {
      this.intersectionObserver.disconnect();
      for (let n of this.manager.tooltipViews)
        this.intersectionObserver.observe(n.dom);
    }
  }
  measureSoon() {
    this.measureTimeout < 0 && (this.measureTimeout = setTimeout(() => {
      this.measureTimeout = -1, this.maybeMeasure();
    }, 50));
  }
  update(n) {
    n.transactions.length && (this.lastTransaction = Date.now());
    let e = this.manager.update(n, this.above);
    e && this.observeIntersection();
    let t = e || n.geometryChanged, i = n.state.facet(ws);
    if (i.position != this.position && !this.madeAbsolute) {
      this.position = i.position;
      for (let s of this.manager.tooltipViews)
        s.dom.style.position = this.position;
      t = !0;
    }
    if (i.parent != this.parent) {
      this.parent && this.container.remove(), this.parent = i.parent, this.createContainer();
      for (let s of this.manager.tooltipViews)
        this.container.appendChild(s.dom);
      t = !0;
    } else this.parent && this.view.themeClasses != this.classes && (this.classes = this.container.className = this.view.themeClasses);
    t && this.maybeMeasure();
  }
  createTooltip(n, e) {
    let t = n.create(this.view), i = e ? e.dom : null;
    if (t.dom.classList.add("cm-tooltip"), n.arrow && !t.dom.querySelector(".cm-tooltip > .cm-tooltip-arrow")) {
      let s = document.createElement("div");
      s.className = "cm-tooltip-arrow", t.dom.appendChild(s);
    }
    return t.dom.style.position = this.position, t.dom.style.top = an, t.dom.style.left = "0px", this.container.insertBefore(t.dom, i), t.mount && t.mount(this.view), this.resizeObserver && this.resizeObserver.observe(t.dom), t;
  }
  destroy() {
    var n, e, t;
    this.view.win.removeEventListener("resize", this.measureSoon);
    for (let i of this.manager.tooltipViews)
      i.dom.remove(), (n = i.destroy) === null || n === void 0 || n.call(i);
    this.parent && this.container.remove(), (e = this.resizeObserver) === null || e === void 0 || e.disconnect(), (t = this.intersectionObserver) === null || t === void 0 || t.disconnect(), clearTimeout(this.measureTimeout);
  }
  readMeasure() {
    let n = 1, e = 1, t = !1;
    if (this.position == "fixed" && this.manager.tooltipViews.length) {
      let { dom: r } = this.manager.tooltipViews[0];
      if (C.safari) {
        let o = r.getBoundingClientRect();
        t = Math.abs(o.top + 1e4) > 1 || Math.abs(o.left) > 1;
      } else
        t = !!r.offsetParent && r.offsetParent != this.container.ownerDocument.body;
    }
    if (t || this.position == "absolute")
      if (this.parent) {
        let r = this.parent.getBoundingClientRect();
        r.width && r.height && (n = r.width / this.parent.offsetWidth, e = r.height / this.parent.offsetHeight);
      } else
        ({ scaleX: n, scaleY: e } = this.view.viewState);
    let i = this.view.scrollDOM.getBoundingClientRect(), s = Pr(this.view);
    return {
      visible: {
        left: i.left + s.left,
        top: i.top + s.top,
        right: i.right - s.right,
        bottom: i.bottom - s.bottom
      },
      parent: this.parent ? this.container.getBoundingClientRect() : this.view.dom.getBoundingClientRect(),
      pos: this.manager.tooltips.map((r, o) => {
        let l = this.manager.tooltipViews[o];
        return l.getCoords ? l.getCoords(r.pos) : this.view.coordsAtPos(r.pos);
      }),
      size: this.manager.tooltipViews.map(({ dom: r }) => r.getBoundingClientRect()),
      space: this.view.state.facet(ws).tooltipSpace(this.view),
      scaleX: n,
      scaleY: e,
      makeAbsolute: t
    };
  }
  writeMeasure(n) {
    var e;
    if (n.makeAbsolute) {
      this.madeAbsolute = !0, this.position = "absolute";
      for (let l of this.manager.tooltipViews)
        l.dom.style.position = "absolute";
    }
    let { visible: t, space: i, scaleX: s, scaleY: r } = n, o = [];
    for (let l = 0; l < this.manager.tooltips.length; l++) {
      let a = this.manager.tooltips[l], h = this.manager.tooltipViews[l], { dom: c } = h, f = n.pos[l], u = n.size[l];
      if (!f || a.clip !== !1 && (f.bottom <= Math.max(t.top, i.top) || f.top >= Math.min(t.bottom, i.bottom) || f.right < Math.max(t.left, i.left) - 0.1 || f.left > Math.min(t.right, i.right) + 0.1)) {
        c.style.top = an;
        continue;
      }
      let d = a.arrow ? h.dom.querySelector(".cm-tooltip-arrow") : null, p = d ? 7 : 0, g = u.right - u.left, m = (e = tl.get(h)) !== null && e !== void 0 ? e : u.bottom - u.top, b = h.offset || pd, v = this.view.textDirection == q.LTR, w = u.width > i.right - i.left ? v ? i.left : i.right - u.width : v ? Math.max(i.left, Math.min(f.left - (d ? 14 : 0) + b.x, i.right - g)) : Math.min(Math.max(i.left, f.left - g + (d ? 14 : 0) - b.x), i.right - g), O = this.above[l];
      !a.strictSide && (O ? f.top - m - p - b.y < i.top : f.bottom + m + p + b.y > i.bottom) && O == i.bottom - f.bottom > f.top - i.top && (O = this.above[l] = !O);
      let k = (O ? f.top - i.top : i.bottom - f.bottom) - p;
      if (k < m && h.resize !== !1) {
        if (k < this.view.defaultLineHeight) {
          c.style.top = an;
          continue;
        }
        tl.set(h, m), c.style.height = (m = k) / r + "px";
      } else c.style.height && (c.style.height = "");
      let S = O ? f.top - m - p - b.y : f.bottom + p + b.y, A = w + g;
      if (h.overlap !== !0)
        for (let L of o)
          L.left < A && L.right > w && L.top < S + m && L.bottom > S && (S = O ? L.top - m - 2 - p : L.bottom + p + 2);
      if (this.position == "absolute" ? (c.style.top = (S - n.parent.top) / r + "px", il(c, (w - n.parent.left) / s)) : (c.style.top = S / r + "px", il(c, w / s)), d) {
        let L = f.left + (v ? b.x : -b.x) - (w + 14 - 7);
        d.style.left = L / s + "px";
      }
      h.overlap !== !0 && o.push({ left: w, top: S, right: A, bottom: S + m }), c.classList.toggle("cm-tooltip-above", O), c.classList.toggle("cm-tooltip-below", !O), h.positioned && h.positioned(n.space);
    }
  }
  maybeMeasure() {
    if (this.manager.tooltips.length && (this.view.inView && this.view.requestMeasure(this.measureReq), this.inView != this.view.inView && (this.inView = this.view.inView, !this.inView)))
      for (let n of this.manager.tooltipViews)
        n.dom.style.top = an;
  }
}, {
  eventObservers: {
    scroll() {
      this.maybeMeasure();
    }
  }
});
function il(n, e) {
  let t = parseInt(n.style.left, 10);
  (isNaN(t) || Math.abs(e - t) > 1) && (n.style.left = e + "px");
}
const dd = /* @__PURE__ */ T.baseTheme({
  ".cm-tooltip": {
    zIndex: 500,
    boxSizing: "border-box"
  },
  "&light .cm-tooltip": {
    border: "1px solid #bbb",
    backgroundColor: "#f5f5f5"
  },
  "&light .cm-tooltip-section:not(:first-child)": {
    borderTop: "1px solid #bbb"
  },
  "&dark .cm-tooltip": {
    backgroundColor: "#333338",
    color: "white"
  },
  ".cm-tooltip-arrow": {
    height: "7px",
    width: "14px",
    position: "absolute",
    zIndex: -1,
    overflow: "hidden",
    "&:before, &:after": {
      content: "''",
      position: "absolute",
      width: 0,
      height: 0,
      borderLeft: "7px solid transparent",
      borderRight: "7px solid transparent"
    },
    ".cm-tooltip-above &": {
      bottom: "-7px",
      "&:before": {
        borderTop: "7px solid #bbb"
      },
      "&:after": {
        borderTop: "7px solid #f5f5f5",
        bottom: "1px"
      }
    },
    ".cm-tooltip-below &": {
      top: "-7px",
      "&:before": {
        borderBottom: "7px solid #bbb"
      },
      "&:after": {
        borderBottom: "7px solid #f5f5f5",
        top: "1px"
      }
    }
  },
  "&dark .cm-tooltip .cm-tooltip-arrow": {
    "&:before": {
      borderTopColor: "#333338",
      borderBottomColor: "#333338"
    },
    "&:after": {
      borderTopColor: "transparent",
      borderBottomColor: "transparent"
    }
  }
}), pd = { x: 0, y: 0 }, Hr = /* @__PURE__ */ M.define({
  enables: [lh, dd]
}), Nn = /* @__PURE__ */ M.define({
  combine: (n) => n.reduce((e, t) => e.concat(t), [])
});
class Yn {
  // Needs to be static so that host tooltip instances always match
  static create(e) {
    return new Yn(e);
  }
  constructor(e) {
    this.view = e, this.mounted = !1, this.dom = document.createElement("div"), this.dom.classList.add("cm-tooltip-hover"), this.manager = new oh(e, Nn, (t, i) => this.createHostedView(t, i), (t) => t.dom.remove());
  }
  createHostedView(e, t) {
    let i = e.create(this.view);
    return i.dom.classList.add("cm-tooltip-section"), this.dom.insertBefore(i.dom, t ? t.dom.nextSibling : this.dom.firstChild), this.mounted && i.mount && i.mount(this.view), i;
  }
  mount(e) {
    for (let t of this.manager.tooltipViews)
      t.mount && t.mount(e);
    this.mounted = !0;
  }
  positioned(e) {
    for (let t of this.manager.tooltipViews)
      t.positioned && t.positioned(e);
  }
  update(e) {
    this.manager.update(e);
  }
  destroy() {
    var e;
    for (let t of this.manager.tooltipViews)
      (e = t.destroy) === null || e === void 0 || e.call(t);
  }
  passProp(e) {
    let t;
    for (let i of this.manager.tooltipViews) {
      let s = i[e];
      if (s !== void 0) {
        if (t === void 0)
          t = s;
        else if (t !== s)
          return;
      }
    }
    return t;
  }
  get offset() {
    return this.passProp("offset");
  }
  get getCoords() {
    return this.passProp("getCoords");
  }
  get overlap() {
    return this.passProp("overlap");
  }
  get resize() {
    return this.passProp("resize");
  }
}
const gd = /* @__PURE__ */ Hr.compute([Nn], (n) => {
  let e = n.facet(Nn);
  return e.length === 0 ? null : {
    pos: Math.min(...e.map((t) => t.pos)),
    end: Math.max(...e.map((t) => {
      var i;
      return (i = t.end) !== null && i !== void 0 ? i : t.pos;
    })),
    create: Yn.create,
    above: e[0].above,
    arrow: e.some((t) => t.arrow)
  };
}), ah = /* @__PURE__ */ M.define();
class md {
  constructor(e, t, i, s, r, o) {
    this.view = e, this.source = t, this.field = i, this.locked = s, this.setHover = r, this.hoverTime = o, this.hoverTimeout = -1, this.restartTimeout = -1, this.pending = null, this.lastMove = { x: 0, y: 0, target: e.dom, time: 0 }, this.checkHover = this.checkHover.bind(this), e.dom.addEventListener("mouseleave", this.mouseleave = this.mouseleave.bind(this)), e.dom.addEventListener("mousemove", this.mousemove = this.mousemove.bind(this));
  }
  update(e) {
    this.pending && (this.pending = null, clearTimeout(this.restartTimeout), this.restartTimeout = setTimeout(() => this.startHover(), 20));
  }
  get active() {
    return this.view.state.field(this.field);
  }
  checkHover() {
    if (this.hoverTimeout = -1, this.active.length)
      return;
    let e = Date.now() - this.lastMove.time;
    e < this.hoverTime ? this.hoverTimeout = setTimeout(this.checkHover, this.hoverTime - e) : this.startHover();
  }
  startHover() {
    clearTimeout(this.restartTimeout);
    let { view: e, lastMove: t } = this, i = e.docView.tile.nearest(t.target);
    if (!i)
      return;
    let s, r = 1;
    if (i.isWidget())
      s = i.posAtStart;
    else {
      if (s = e.posAtCoords(t), s == null)
        return;
      let o = e.coordsAtPos(s);
      if (!o || t.y < o.top || t.y > o.bottom || t.x < o.left - e.defaultCharacterWidth || t.x > o.right + e.defaultCharacterWidth)
        return;
      let l = e.bidiSpans(e.state.doc.lineAt(s)).find((h) => h.from <= s && h.to >= s), a = l && l.dir == q.RTL ? -1 : 1;
      r = t.x < o.left ? -a : a;
    }
    this.activateHover(e, s, r);
  }
  activateHover(e, t, i, s) {
    let r = this.source(e, t, i), o = (l) => {
      if (l && !(Array.isArray(l) && !l.length)) {
        let a = Array.isArray(l) ? l : [l];
        s && this.locked.set(a, s), e.dispatch({ effects: this.setHover.of(a) });
      }
    };
    if (r && "then" in r) {
      let l = this.pending = { pos: t };
      r.then((a) => {
        this.pending == l && (this.pending = null, o(a));
      }, (a) => Pe(e.state, a, "hover tooltip"));
    } else
      o(r);
  }
  get tooltip() {
    let e = this.view.plugin(lh), t = e ? e.manager.tooltips.findIndex((i) => i.create == Yn.create) : -1;
    return t > -1 ? e.manager.tooltipViews[t] : null;
  }
  mousemove(e) {
    var t, i;
    this.lastMove = { x: e.clientX, y: e.clientY, target: e.target, time: Date.now() }, this.hoverTimeout < 0 && (this.hoverTimeout = setTimeout(this.checkHover, this.hoverTime));
    let { active: s, tooltip: r } = this;
    if (s.length && !this.locked.has(s) && r && !bd(r.dom, e) || this.pending) {
      let { pos: o } = s[0] || this.pending, l = (i = (t = s[0]) === null || t === void 0 ? void 0 : t.end) !== null && i !== void 0 ? i : o;
      (o == l ? this.view.posAtCoords(this.lastMove) != o : !yd(this.view, o, l, e.clientX, e.clientY)) && (this.view.dispatch({ effects: this.setHover.of([]) }), this.pending = null);
    }
  }
  mouseleave(e) {
    clearTimeout(this.hoverTimeout), this.hoverTimeout = -1;
    let { active: t } = this;
    if (t.length && !this.locked.has(t)) {
      let { tooltip: i } = this;
      i && i.dom.contains(e.relatedTarget) ? this.watchTooltipLeave(i.dom) : this.view.dispatch({ effects: this.setHover.of([]) });
    }
  }
  watchTooltipLeave(e) {
    let t = (i) => {
      e.removeEventListener("mouseleave", t);
      let { active: s } = this;
      s.length && !this.locked.has(s) && !this.view.dom.contains(i.relatedTarget) && this.view.dispatch({ effects: this.setHover.of([]) });
    };
    e.addEventListener("mouseleave", t);
  }
  destroy() {
    clearTimeout(this.hoverTimeout), clearTimeout(this.restartTimeout), this.view.dom.removeEventListener("mouseleave", this.mouseleave), this.view.dom.removeEventListener("mousemove", this.mousemove);
  }
}
const hn = 4;
function bd(n, e) {
  let { left: t, right: i, top: s, bottom: r } = n.getBoundingClientRect(), o;
  if (o = n.querySelector(".cm-tooltip-arrow")) {
    let l = o.getBoundingClientRect();
    s = Math.min(l.top, s), r = Math.max(l.bottom, r);
  }
  return e.clientX >= t - hn && e.clientX <= i + hn && e.clientY >= s - hn && e.clientY <= r + hn;
}
function yd(n, e, t, i, s, r) {
  let o = n.scrollDOM.getBoundingClientRect(), l = n.documentTop + n.documentPadding.top + n.contentHeight;
  if (o.left > i || o.right < i || o.top > s || Math.min(o.bottom, l) < s)
    return !1;
  let a = n.posAtCoords({ x: i, y: s }, !1);
  return a >= e && a <= t;
}
function wd(n, e = {}) {
  let t = W.define(), i = /* @__PURE__ */ new WeakMap(), s = Ae.define({
    create() {
      return [];
    },
    update(o, l) {
      let a = i.get(o);
      if (o.length && (e.hideOnChange && (l.docChanged || l.selection) ? o = [] : a && a(l) ? o = [] : e.hideOn && (o = o.filter((h) => !e.hideOn(l, h)))), l.docChanged && o.length) {
        let h = [];
        for (let c of o) {
          let f = l.changes.mapPos(c.pos, -1, pe.TrackDel);
          if (f != null) {
            let u = Object.assign(/* @__PURE__ */ Object.create(null), c);
            u.pos = f, u.end != null && (u.end = l.changes.mapPos(u.end)), h.push(u);
          }
        }
        o = h;
      }
      for (let h of l.effects)
        h.is(t) && (o = h.value, a = void 0), (h.is(xd) && !h.value || h.value == s) && (o = []);
      return o.length && a && i.set(o, a), o;
    },
    provide: (o) => Nn.from(o)
  });
  const r = be.define((o) => new md(
    o,
    n,
    s,
    i,
    t,
    e.hoverTime || 300
    /* Hover.Time */
  ));
  return {
    active: s,
    extension: [
      s,
      r,
      ah.of(r),
      gd
    ]
  };
}
function vd(n, e, t, i = {}) {
  var s;
  let r = n.state.facet(ah).map((o) => n.plugin(o)).filter((o) => !!o);
  if (i.tooltip && i.tooltip.active) {
    let o = r.find((l) => l.field == i.tooltip.active);
    o && (r = [o]);
  }
  for (let o of r)
    o.activateHover(n, e, t, (s = i.until) !== null && s !== void 0 ? s : (() => !1));
}
const xd = /* @__PURE__ */ W.define(), nl = /* @__PURE__ */ M.define({
  combine(n) {
    let e, t;
    for (let i of n)
      e = e || i.topContainer, t = t || i.bottomContainer;
    return { topContainer: e, bottomContainer: t };
  }
});
function kd(n, e) {
  let t = n.plugin(hh), i = t ? t.specs.indexOf(e) : -1;
  return i > -1 ? t.panels[i] : null;
}
const hh = /* @__PURE__ */ be.fromClass(class {
  constructor(n) {
    this.input = n.state.facet(hr), this.specs = this.input.filter((t) => t), this.panels = this.specs.map((t) => t(n));
    let e = n.state.facet(nl);
    this.top = new cn(n, !0, e.topContainer), this.bottom = new cn(n, !1, e.bottomContainer), this.top.sync(this.panels.filter((t) => t.top)), this.bottom.sync(this.panels.filter((t) => !t.top));
    for (let t of this.panels)
      t.dom.classList.add("cm-panel"), t.mount && t.mount();
  }
  update(n) {
    let e = n.state.facet(nl);
    this.top.container != e.topContainer && (this.top.sync([]), this.top = new cn(n.view, !0, e.topContainer)), this.bottom.container != e.bottomContainer && (this.bottom.sync([]), this.bottom = new cn(n.view, !1, e.bottomContainer)), this.top.syncClasses(), this.bottom.syncClasses();
    let t = n.state.facet(hr);
    if (t != this.input) {
      let i = t.filter((a) => a), s = [], r = [], o = [], l = [];
      for (let a of i) {
        let h = this.specs.indexOf(a), c;
        h < 0 ? (c = a(n.view), l.push(c)) : (c = this.panels[h], c.update && c.update(n)), s.push(c), (c.top ? r : o).push(c);
      }
      this.specs = i, this.panels = s, this.top.sync(r), this.bottom.sync(o);
      for (let a of l)
        a.dom.classList.add("cm-panel"), a.mount && a.mount();
    } else
      for (let i of this.panels)
        i.update && i.update(n);
  }
  destroy() {
    this.top.sync([]), this.bottom.sync([]);
  }
}, {
  provide: (n) => T.scrollMargins.of((e) => {
    let t = e.plugin(n);
    return t && { top: t.top.scrollMargin(), bottom: t.bottom.scrollMargin() };
  })
});
class cn {
  constructor(e, t, i) {
    this.view = e, this.top = t, this.container = i, this.dom = void 0, this.classes = "", this.panels = [], this.syncClasses();
  }
  sync(e) {
    for (let t of this.panels)
      t.destroy && e.indexOf(t) < 0 && t.destroy();
    this.panels = e, this.syncDOM();
  }
  syncDOM() {
    if (this.panels.length == 0) {
      this.dom && (this.dom.remove(), this.dom = void 0);
      return;
    }
    if (!this.dom) {
      this.dom = document.createElement("div"), this.dom.className = this.top ? "cm-panels cm-panels-top" : "cm-panels cm-panels-bottom", this.dom.style[this.top ? "top" : "bottom"] = "0";
      let t = this.container || this.view.dom;
      t.insertBefore(this.dom, this.top ? t.firstChild : null);
    }
    let e = this.dom.firstChild;
    for (let t of this.panels)
      if (t.dom.parentNode == this.dom) {
        for (; e != t.dom; )
          e = sl(e);
        e = e.nextSibling;
      } else
        this.dom.insertBefore(t.dom, e);
    for (; e; )
      e = sl(e);
  }
  scrollMargin() {
    return !this.dom || this.container ? 0 : Math.max(0, this.top ? this.dom.getBoundingClientRect().bottom - Math.max(0, this.view.scrollDOM.getBoundingClientRect().top) : Math.min(innerHeight, this.view.scrollDOM.getBoundingClientRect().bottom) - this.dom.getBoundingClientRect().top);
  }
  syncClasses() {
    if (!(!this.container || this.classes == this.view.themeClasses)) {
      for (let e of this.classes.split(" "))
        e && this.container.classList.remove(e);
      for (let e of (this.classes = this.view.themeClasses).split(" "))
        e && this.container.classList.add(e);
    }
  }
}
function sl(n) {
  let e = n.nextSibling;
  return n.remove(), e;
}
const hr = /* @__PURE__ */ M.define({
  enables: hh
});
class st extends Rt {
  /**
  @internal
  */
  compare(e) {
    return this == e || this.constructor == e.constructor && this.eq(e);
  }
  /**
  Compare this marker to another marker of the same type.
  */
  eq(e) {
    return !1;
  }
  /**
  Called if the marker has a `toDOM` method and its representation
  was removed from a gutter.
  */
  destroy(e) {
  }
}
st.prototype.elementClass = "";
st.prototype.toDOM = void 0;
st.prototype.mapMode = pe.TrackBefore;
st.prototype.startSide = st.prototype.endSide = -1;
st.prototype.point = !0;
const kn = /* @__PURE__ */ M.define(), Sd = /* @__PURE__ */ M.define(), Ad = {
  class: "",
  renderEmptyElements: !1,
  elementStyle: "",
  markers: () => B.empty,
  lineMarker: () => null,
  widgetMarker: () => null,
  lineMarkerChange: null,
  initialSpacer: null,
  updateSpacer: null,
  domEventHandlers: {},
  side: "before"
}, Ai = /* @__PURE__ */ M.define();
function Cd(n) {
  return [ch(), Ai.of({ ...Ad, ...n })];
}
const rl = /* @__PURE__ */ M.define({
  combine: (n) => n.some((e) => e)
});
function ch(n) {
  return [
    Md
  ];
}
const Md = /* @__PURE__ */ be.fromClass(class {
  constructor(n) {
    this.view = n, this.domAfter = null, this.prevViewport = n.viewport, this.dom = document.createElement("div"), this.dom.className = "cm-gutters cm-gutters-before", this.dom.setAttribute("aria-hidden", "true"), this.dom.style.minHeight = this.view.contentHeight / this.view.scaleY + "px", this.gutters = n.state.facet(Ai).map((e) => new ll(n, e)), this.fixed = !n.state.facet(rl);
    for (let e of this.gutters)
      e.config.side == "after" ? this.getDOMAfter().appendChild(e.dom) : this.dom.appendChild(e.dom);
    this.fixed && (this.dom.style.position = "sticky"), this.syncGutters(!1), n.scrollDOM.insertBefore(this.dom, n.contentDOM);
  }
  getDOMAfter() {
    return this.domAfter || (this.domAfter = document.createElement("div"), this.domAfter.className = "cm-gutters cm-gutters-after", this.domAfter.setAttribute("aria-hidden", "true"), this.domAfter.style.minHeight = this.view.contentHeight / this.view.scaleY + "px", this.domAfter.style.position = this.fixed ? "sticky" : "", this.view.scrollDOM.appendChild(this.domAfter)), this.domAfter;
  }
  update(n) {
    if (this.updateGutters(n)) {
      let e = this.prevViewport, t = n.view.viewport, i = Math.min(e.to, t.to) - Math.max(e.from, t.from);
      this.syncGutters(i < (t.to - t.from) * 0.8);
    }
    if (n.geometryChanged) {
      let e = this.view.contentHeight / this.view.scaleY + "px";
      this.dom.style.minHeight = e, this.domAfter && (this.domAfter.style.minHeight = e);
    }
    this.view.state.facet(rl) != !this.fixed && (this.fixed = !this.fixed, this.dom.style.position = this.fixed ? "sticky" : "", this.domAfter && (this.domAfter.style.position = this.fixed ? "sticky" : "")), this.prevViewport = n.view.viewport;
  }
  syncGutters(n) {
    let e = this.dom.nextSibling;
    n && (this.dom.remove(), this.domAfter && this.domAfter.remove());
    let t = B.iter(this.view.state.facet(kn), this.view.viewport.from), i = [], s = this.gutters.map((r) => new Td(r, this.view.viewport, -this.view.documentPadding.top));
    for (let r of this.view.viewportLineBlocks)
      if (i.length && (i = []), Array.isArray(r.type)) {
        let o = !0;
        for (let l of r.type)
          if (l.type == re.Text && o) {
            cr(t, i, l.from);
            for (let a of s)
              a.line(this.view, l, i);
            o = !1;
          } else if (l.widget)
            for (let a of s)
              a.widget(this.view, l);
      } else if (r.type == re.Text) {
        cr(t, i, r.from);
        for (let o of s)
          o.line(this.view, r, i);
      } else if (r.widget)
        for (let o of s)
          o.widget(this.view, r);
    for (let r of s)
      r.finish();
    n && (this.view.scrollDOM.insertBefore(this.dom, e), this.domAfter && this.view.scrollDOM.appendChild(this.domAfter));
  }
  updateGutters(n) {
    let e = n.startState.facet(Ai), t = n.state.facet(Ai), i = n.docChanged || n.heightChanged || n.viewportChanged || !B.eq(n.startState.facet(kn), n.state.facet(kn), n.view.viewport.from, n.view.viewport.to);
    if (e == t)
      for (let s of this.gutters)
        s.update(n) && (i = !0);
    else {
      i = !0;
      let s = [];
      for (let r of t) {
        let o = e.indexOf(r);
        o < 0 ? s.push(new ll(this.view, r)) : (this.gutters[o].update(n), s.push(this.gutters[o]));
      }
      for (let r of this.gutters)
        r.dom.remove(), s.indexOf(r) < 0 && r.destroy();
      for (let r of s)
        r.config.side == "after" ? this.getDOMAfter().appendChild(r.dom) : this.dom.appendChild(r.dom);
      this.gutters = s;
    }
    return i;
  }
  destroy() {
    for (let n of this.gutters)
      n.destroy();
    this.dom.remove(), this.domAfter && this.domAfter.remove();
  }
}, {
  provide: (n) => T.scrollMargins.of((e) => {
    let t = e.plugin(n);
    if (!t || t.gutters.length == 0 || !t.fixed)
      return null;
    let i = t.dom.offsetWidth * e.scaleX, s = t.domAfter ? t.domAfter.offsetWidth * e.scaleX : 0;
    return e.textDirection == q.LTR ? { left: i, right: s } : { right: i, left: s };
  })
});
function ol(n) {
  return Array.isArray(n) ? n : [n];
}
function cr(n, e, t) {
  for (; n.value && n.from <= t; )
    n.from == t && e.push(n.value), n.next();
}
class Td {
  constructor(e, t, i) {
    this.gutter = e, this.height = i, this.i = 0, this.cursor = B.iter(e.markers, t.from);
  }
  addElement(e, t, i) {
    let { gutter: s } = this, r = (t.top - this.height) / e.scaleY, o = t.height / e.scaleY;
    if (this.i == s.elements.length) {
      let l = new fh(e, o, r, i);
      s.elements.push(l), s.dom.appendChild(l.dom);
    } else
      s.elements[this.i].update(e, o, r, i);
    this.height = t.bottom, this.i++;
  }
  line(e, t, i) {
    let s = [];
    cr(this.cursor, s, t.from), i.length && (s = s.concat(i));
    let r = this.gutter.config.lineMarker(e, t, s);
    r && s.unshift(r);
    let o = this.gutter;
    s.length == 0 && !o.config.renderEmptyElements || this.addElement(e, t, s);
  }
  widget(e, t) {
    let i = this.gutter.config.widgetMarker(e, t.widget, t), s = i ? [i] : null;
    for (let r of e.state.facet(Sd)) {
      let o = r(e, t.widget, t);
      o && (s || (s = [])).push(o);
    }
    s && this.addElement(e, t, s);
  }
  finish() {
    let e = this.gutter;
    for (; e.elements.length > this.i; ) {
      let t = e.elements.pop();
      e.dom.removeChild(t.dom), t.destroy();
    }
  }
}
class ll {
  constructor(e, t) {
    this.view = e, this.config = t, this.elements = [], this.spacer = null, this.dom = document.createElement("div"), this.dom.className = "cm-gutter" + (this.config.class ? " " + this.config.class : "");
    for (let i in t.domEventHandlers)
      this.dom.addEventListener(i, (s) => {
        let r = s.target, o;
        if (r != this.dom && this.dom.contains(r)) {
          for (; r.parentNode != this.dom; )
            r = r.parentNode;
          let a = r.getBoundingClientRect();
          o = (a.top + a.bottom) / 2;
        } else
          o = s.clientY;
        let l = e.lineBlockAtHeight(o - e.documentTop);
        t.domEventHandlers[i](e, l, s) && s.preventDefault();
      });
    this.markers = ol(t.markers(e)), t.initialSpacer && (this.spacer = new fh(e, 0, 0, [t.initialSpacer(e)]), this.dom.appendChild(this.spacer.dom), this.spacer.dom.style.cssText += "visibility: hidden; pointer-events: none");
  }
  update(e) {
    let t = this.markers;
    if (this.markers = ol(this.config.markers(e.view)), this.spacer && this.config.updateSpacer) {
      let s = this.config.updateSpacer(this.spacer.markers[0], e);
      s != this.spacer.markers[0] && this.spacer.update(e.view, 0, 0, [s]);
    }
    let i = e.view.viewport;
    return !B.eq(this.markers, t, i.from, i.to) || (this.config.lineMarkerChange ? this.config.lineMarkerChange(e) : !1);
  }
  destroy() {
    for (let e of this.elements)
      e.destroy();
  }
}
class fh {
  constructor(e, t, i, s) {
    this.height = -1, this.above = 0, this.markers = [], this.dom = document.createElement("div"), this.dom.className = "cm-gutterElement", this.update(e, t, i, s);
  }
  update(e, t, i, s) {
    this.height != t && (this.height = t, this.dom.style.height = t + "px"), this.above != i && (this.dom.style.marginTop = (this.above = i) ? i + "px" : ""), Od(this.markers, s) || this.setMarkers(e, s);
  }
  setMarkers(e, t) {
    let i = "cm-gutterElement", s = this.dom.firstChild;
    for (let r = 0, o = 0; ; ) {
      let l = o, a = r < t.length ? t[r++] : null, h = !1;
      if (a) {
        let c = a.elementClass;
        c && (i += " " + c);
        for (let f = o; f < this.markers.length; f++)
          if (this.markers[f].compare(a)) {
            l = f, h = !0;
            break;
          }
      } else
        l = this.markers.length;
      for (; o < l; ) {
        let c = this.markers[o++];
        if (c.toDOM) {
          c.destroy(s);
          let f = s.nextSibling;
          s.remove(), s = f;
        }
      }
      if (!a)
        break;
      a.toDOM && (h ? s = s.nextSibling : this.dom.insertBefore(a.toDOM(e), s)), h && o++;
    }
    this.dom.className = i, this.markers = t;
  }
  destroy() {
    this.setMarkers(null, []);
  }
}
function Od(n, e) {
  if (n.length != e.length)
    return !1;
  for (let t = 0; t < n.length; t++)
    if (!n[t].compare(e[t]))
      return !1;
  return !0;
}
const Dd = /* @__PURE__ */ M.define(), Ed = /* @__PURE__ */ M.define(), _t = /* @__PURE__ */ M.define({
  combine(n) {
    return _i(n, { formatNumber: String, domEventHandlers: {} }, {
      domEventHandlers(e, t) {
        let i = Object.assign({}, e);
        for (let s in t) {
          let r = i[s], o = t[s];
          i[s] = r ? (l, a, h) => r(l, a, h) || o(l, a, h) : o;
        }
        return i;
      }
    });
  }
});
class vs extends st {
  constructor(e) {
    super(), this.number = e;
  }
  eq(e) {
    return this.number == e.number;
  }
  toDOM() {
    return document.createTextNode(this.number);
  }
}
function xs(n, e) {
  return n.state.facet(_t).formatNumber(e, n.state);
}
const Ld = /* @__PURE__ */ Ai.compute([_t], (n) => ({
  class: "cm-lineNumbers",
  renderEmptyElements: !1,
  markers(e) {
    return e.state.facet(Dd);
  },
  lineMarker(e, t, i) {
    return i.some((s) => s.toDOM) ? null : new vs(xs(e, e.state.doc.lineAt(t.from).number));
  },
  widgetMarker: (e, t, i) => {
    for (let s of e.state.facet(Ed)) {
      let r = s(e, t, i);
      if (r)
        return r;
    }
    return null;
  },
  lineMarkerChange: (e) => e.startState.facet(_t) != e.state.facet(_t),
  initialSpacer(e) {
    return new vs(xs(e, al(e.state.doc.lines)));
  },
  updateSpacer(e, t) {
    let i = xs(t.view, al(t.view.state.doc.lines));
    return i == e.number ? e : new vs(i);
  },
  domEventHandlers: n.facet(_t).domEventHandlers,
  side: "before"
}));
function Rd(n = {}) {
  return [
    _t.of(n),
    ch(),
    Ld
  ];
}
function al(n) {
  let e = 9;
  for (; e < n; )
    e = e * 10 + 9;
  return e;
}
const Bd = /* @__PURE__ */ new class extends st {
  constructor() {
    super(...arguments), this.elementClass = "cm-activeLineGutter";
  }
}(), Pd = /* @__PURE__ */ kn.compute(["selection"], (n) => {
  let e = [], t = -1;
  for (let i of n.selection.ranges) {
    let s = n.doc.lineAt(i.head).from;
    s > t && (t = s, e.push(Bd.range(s)));
  }
  return B.of(e);
});
function Id() {
  return Pd;
}
const $d = 1024;
let Nd = 0;
class ks {
  constructor(e, t) {
    this.from = e, this.to = t;
  }
}
class I {
  /**
  Create a new node prop type.
  */
  constructor(e = {}) {
    this.id = Nd++, this.perNode = !!e.perNode, this.deserialize = e.deserialize || (() => {
      throw new Error("This node type doesn't define a deserialize function");
    }), this.combine = e.combine || null;
  }
  /**
  This is meant to be used with
  [`NodeSet.extend`](#common.NodeSet.extend) or
  [`LRParser.configure`](#lr.ParserConfig.props) to compute
  prop values for each node type in the set. Takes a [match
  object](#common.NodeType^match) or function that returns undefined
  if the node type doesn't get this prop, and the prop's value if
  it does.
  */
  add(e) {
    if (this.perNode)
      throw new RangeError("Can't add per-node props to node types");
    return typeof e != "function" && (e = we.match(e)), (t) => {
      let i = e(t);
      return i === void 0 ? null : [this, i];
    };
  }
}
I.closedBy = new I({ deserialize: (n) => n.split(" ") });
I.openedBy = new I({ deserialize: (n) => n.split(" ") });
I.group = new I({ deserialize: (n) => n.split(" ") });
I.isolate = new I({ deserialize: (n) => {
  if (n && n != "rtl" && n != "ltr" && n != "auto")
    throw new RangeError("Invalid value for isolate: " + n);
  return n || "auto";
} });
I.contextHash = new I({ perNode: !0 });
I.lookAhead = new I({ perNode: !0 });
I.mounted = new I({ perNode: !0 });
class Ci {
  constructor(e, t, i, s = !1) {
    this.tree = e, this.overlay = t, this.parser = i, this.bracketed = s;
  }
  /**
  @internal
  */
  static get(e) {
    return e && e.props && e.props[I.mounted.id];
  }
}
const Hd = /* @__PURE__ */ Object.create(null);
class we {
  /**
  @internal
  */
  constructor(e, t, i, s = 0) {
    this.name = e, this.props = t, this.id = i, this.flags = s;
  }
  /**
  Define a node type.
  */
  static define(e) {
    let t = e.props && e.props.length ? /* @__PURE__ */ Object.create(null) : Hd, i = (e.top ? 1 : 0) | (e.skipped ? 2 : 0) | (e.error ? 4 : 0) | (e.name == null ? 8 : 0), s = new we(e.name || "", t, e.id, i);
    if (e.props) {
      for (let r of e.props)
        if (Array.isArray(r) || (r = r(s)), r) {
          if (r[0].perNode)
            throw new RangeError("Can't store a per-node prop on a node type");
          t[r[0].id] = r[1];
        }
    }
    return s;
  }
  /**
  Retrieves a node prop for this type. Will return `undefined` if
  the prop isn't present on this node.
  */
  prop(e) {
    return this.props[e.id];
  }
  /**
  True when this is the top node of a grammar.
  */
  get isTop() {
    return (this.flags & 1) > 0;
  }
  /**
  True when this node is produced by a skip rule.
  */
  get isSkipped() {
    return (this.flags & 2) > 0;
  }
  /**
  Indicates whether this is an error node.
  */
  get isError() {
    return (this.flags & 4) > 0;
  }
  /**
  When true, this node type doesn't correspond to a user-declared
  named node, for example because it is used to cache repetition.
  */
  get isAnonymous() {
    return (this.flags & 8) > 0;
  }
  /**
  Returns true when this node's name or one of its
  [groups](#common.NodeProp^group) matches the given string.
  */
  is(e) {
    if (typeof e == "string") {
      if (this.name == e)
        return !0;
      let t = this.prop(I.group);
      return t ? t.indexOf(e) > -1 : !1;
    }
    return this.id == e;
  }
  /**
  Create a function from node types to arbitrary values by
  specifying an object whose property names are node or
  [group](#common.NodeProp^group) names. Often useful with
  [`NodeProp.add`](#common.NodeProp.add). You can put multiple
  names, separated by spaces, in a single property name to map
  multiple node names to a single value.
  */
  static match(e) {
    let t = /* @__PURE__ */ Object.create(null);
    for (let i in e)
      for (let s of i.split(" "))
        t[s] = e[i];
    return (i) => {
      for (let s = i.prop(I.group), r = -1; r < (s ? s.length : 0); r++) {
        let o = t[r < 0 ? i.name : s[r]];
        if (o)
          return o;
      }
    };
  }
}
we.none = new we(
  "",
  /* @__PURE__ */ Object.create(null),
  0,
  8
  /* NodeFlag.Anonymous */
);
class Vr {
  /**
  Create a set with the given types. The `id` property of each
  type should correspond to its position within the array.
  */
  constructor(e) {
    this.types = e;
    for (let t = 0; t < e.length; t++)
      if (e[t].id != t)
        throw new RangeError("Node type ids should correspond to array positions when creating a node set");
  }
  /**
  Create a copy of this set with some node properties added. The
  arguments to this method can be created with
  [`NodeProp.add`](#common.NodeProp.add).
  */
  extend(...e) {
    let t = [];
    for (let i of this.types) {
      let s = null;
      for (let r of e) {
        let o = r(i);
        if (o) {
          s || (s = Object.assign({}, i.props));
          let l = o[1], a = o[0];
          a.combine && a.id in s && (l = a.combine(s[a.id], l)), s[a.id] = l;
        }
      }
      t.push(s ? new we(i.name, s, i.id, i.flags) : i);
    }
    return new Vr(t);
  }
}
const fn = /* @__PURE__ */ new WeakMap(), hl = /* @__PURE__ */ new WeakMap();
var X;
(function(n) {
  n[n.ExcludeBuffers = 1] = "ExcludeBuffers", n[n.IncludeAnonymous = 2] = "IncludeAnonymous", n[n.IgnoreMounts = 4] = "IgnoreMounts", n[n.IgnoreOverlays = 8] = "IgnoreOverlays", n[n.EnterBracketed = 16] = "EnterBracketed";
})(X || (X = {}));
class G {
  /**
  Construct a new tree. See also [`Tree.build`](#common.Tree^build).
  */
  constructor(e, t, i, s, r) {
    if (this.type = e, this.children = t, this.positions = i, this.length = s, this.props = null, r && r.length) {
      this.props = /* @__PURE__ */ Object.create(null);
      for (let [o, l] of r)
        this.props[typeof o == "number" ? o : o.id] = l;
    }
  }
  /**
  @internal
  */
  toString() {
    let e = Ci.get(this);
    if (e && !e.overlay)
      return e.tree.toString();
    let t = "";
    for (let i of this.children) {
      let s = i.toString();
      s && (t && (t += ","), t += s);
    }
    return this.type.name ? (/\W/.test(this.type.name) && !this.type.isError ? JSON.stringify(this.type.name) : this.type.name) + (t.length ? "(" + t + ")" : "") : t;
  }
  /**
  Get a [tree cursor](#common.TreeCursor) positioned at the top of
  the tree. Mode can be used to [control](#common.IterMode) which
  nodes the cursor visits.
  */
  cursor(e = 0) {
    return new ur(this.topNode, e);
  }
  /**
  Get a [tree cursor](#common.TreeCursor) pointing into this tree
  at the given position and side (see
  [`moveTo`](#common.TreeCursor.moveTo).
  */
  cursorAt(e, t = 0, i = 0) {
    let s = fn.get(this) || this.topNode, r = new ur(s);
    return r.moveTo(e, t), fn.set(this, r._tree), r;
  }
  /**
  Get a [syntax node](#common.SyntaxNode) object for the top of the
  tree.
  */
  get topNode() {
    return new Ee(this, 0, 0, null);
  }
  /**
  Get the [syntax node](#common.SyntaxNode) at the given position.
  If `side` is -1, this will move into nodes that end at the
  position. If 1, it'll move into nodes that start at the
  position. With 0, it'll only enter nodes that cover the position
  from both sides.
  
  Note that this will not enter
  [overlays](#common.MountedTree.overlay), and you often want
  [`resolveInner`](#common.Tree.resolveInner) instead.
  */
  resolve(e, t = 0) {
    let i = Pi(fn.get(this) || this.topNode, e, t, !1);
    return fn.set(this, i), i;
  }
  /**
  Like [`resolve`](#common.Tree.resolve), but will enter
  [overlaid](#common.MountedTree.overlay) nodes, producing a syntax node
  pointing into the innermost overlaid tree at the given position
  (with parent links going through all parent structure, including
  the host trees).
  */
  resolveInner(e, t = 0) {
    let i = Pi(hl.get(this) || this.topNode, e, t, !0);
    return hl.set(this, i), i;
  }
  /**
  In some situations, it can be useful to iterate through all
  nodes around a position, including those in overlays that don't
  directly cover the position. This method gives you an iterator
  that will produce all nodes, from small to big, around the given
  position.
  */
  resolveStack(e, t = 0) {
    return Wd(this, e, t);
  }
  /**
  Iterate over the tree and its children, calling `enter` for any
  node that touches the `from`/`to` region (if given) before
  running over such a node's children, and `leave` (if given) when
  leaving the node. When `enter` returns `false`, that node will
  not have its children iterated over (or `leave` called).
  */
  iterate(e) {
    let { enter: t, leave: i, from: s = 0, to: r = this.length } = e, o = e.mode || 0, l = (o & X.IncludeAnonymous) > 0;
    for (let a = this.cursor(o | X.IncludeAnonymous); ; ) {
      let h = !1;
      if (a.from <= r && a.to >= s && (!l && a.type.isAnonymous || t(a) !== !1)) {
        if (a.firstChild())
          continue;
        h = !0;
      }
      for (; h && i && (l || !a.type.isAnonymous) && i(a), !a.nextSibling(); ) {
        if (!a.parent())
          return;
        h = !0;
      }
    }
  }
  /**
  Get the value of the given [node prop](#common.NodeProp) for this
  node. Works with both per-node and per-type props.
  */
  prop(e) {
    return e.perNode ? this.props ? this.props[e.id] : void 0 : this.type.prop(e);
  }
  /**
  Returns the node's [per-node props](#common.NodeProp.perNode) in a
  format that can be passed to the [`Tree`](#common.Tree)
  constructor.
  */
  get propValues() {
    let e = [];
    if (this.props)
      for (let t in this.props)
        e.push([+t, this.props[t]]);
    return e;
  }
  /**
  Balance the direct children of this tree, producing a copy of
  which may have children grouped into subtrees with type
  [`NodeType.none`](#common.NodeType^none).
  */
  balance(e = {}) {
    return this.children.length <= 8 ? this : _r(we.none, this.children, this.positions, 0, this.children.length, 0, this.length, (t, i, s) => new G(this.type, t, i, s, this.propValues), e.makeTree || ((t, i, s) => new G(we.none, t, i, s)));
  }
  /**
  Build a tree from a postfix-ordered buffer of node information,
  or a cursor over such a buffer.
  */
  static build(e) {
    return _d(e);
  }
}
G.empty = new G(we.none, [], [], 0);
class Fr {
  constructor(e, t) {
    this.buffer = e, this.index = t;
  }
  get id() {
    return this.buffer[this.index - 4];
  }
  get start() {
    return this.buffer[this.index - 3];
  }
  get end() {
    return this.buffer[this.index - 2];
  }
  get size() {
    return this.buffer[this.index - 1];
  }
  get pos() {
    return this.index;
  }
  next() {
    this.index -= 4;
  }
  fork() {
    return new Fr(this.buffer, this.index);
  }
}
class mt {
  /**
  Create a tree buffer.
  */
  constructor(e, t, i) {
    this.buffer = e, this.length = t, this.set = i;
  }
  /**
  @internal
  */
  get type() {
    return we.none;
  }
  /**
  @internal
  */
  toString() {
    let e = [];
    for (let t = 0; t < this.buffer.length; )
      e.push(this.childString(t)), t = this.buffer[t + 3];
    return e.join(",");
  }
  /**
  @internal
  */
  childString(e) {
    let t = this.buffer[e], i = this.buffer[e + 3], s = this.set.types[t], r = s.name;
    if (/\W/.test(r) && !s.isError && (r = JSON.stringify(r)), e += 4, i == e)
      return r;
    let o = [];
    for (; e < i; )
      o.push(this.childString(e)), e = this.buffer[e + 3];
    return r + "(" + o.join(",") + ")";
  }
  /**
  @internal
  */
  findChild(e, t, i, s, r) {
    let { buffer: o } = this, l = -1;
    for (let a = e; a != t && !(uh(r, s, o[a + 1], o[a + 2]) && (l = a, i > 0)); a = o[a + 3])
      ;
    return l;
  }
  /**
  @internal
  */
  slice(e, t, i) {
    let s = this.buffer, r = new Uint16Array(t - e), o = 0;
    for (let l = e, a = 0; l < t; ) {
      r[a++] = s[l++], r[a++] = s[l++] - i;
      let h = r[a++] = s[l++] - i;
      r[a++] = s[l++] - e, o = Math.max(o, h);
    }
    return new mt(r, o, this.set);
  }
}
function uh(n, e, t, i) {
  switch (n) {
    case -2:
      return t < e;
    case -1:
      return i >= e && t < e;
    case 0:
      return t < e && i > e;
    case 1:
      return t <= e && i > e;
    case 2:
      return i > e;
    case 4:
      return !0;
  }
}
function Pi(n, e, t, i) {
  for (var s; n.from == n.to || (t < 1 ? n.from >= e : n.from > e) || (t > -1 ? n.to <= e : n.to < e); ) {
    let o = !i && n instanceof Ee && n.index < 0 ? null : n.parent;
    if (!o)
      return n;
    n = o;
  }
  let r = i ? 0 : X.IgnoreOverlays;
  if (i)
    for (let o = n, l = o.parent; l; o = l, l = o.parent)
      o instanceof Ee && o.index < 0 && ((s = l.enter(e, t, r)) === null || s === void 0 ? void 0 : s.from) != o.from && (n = l);
  for (; ; ) {
    let o = n.enter(e, t, r);
    if (!o)
      return n;
    n = o;
  }
}
class dh {
  cursor(e = 0) {
    return new ur(this, e);
  }
  getChild(e, t = null, i = null) {
    let s = cl(this, e, t, i);
    return s.length ? s[0] : null;
  }
  getChildren(e, t = null, i = null) {
    return cl(this, e, t, i);
  }
  resolve(e, t = 0) {
    return Pi(this, e, t, !1);
  }
  resolveInner(e, t = 0) {
    return Pi(this, e, t, !0);
  }
  matchContext(e) {
    return fr(this.parent, e);
  }
  enterUnfinishedNodesBefore(e) {
    let t = this.childBefore(e), i = this;
    for (; t; ) {
      let s = t.lastChild;
      if (!s || s.to != t.to)
        break;
      s.type.isError && s.from == s.to ? (i = t, t = s.prevSibling) : t = s;
    }
    return i;
  }
  get node() {
    return this;
  }
  get next() {
    return this.parent;
  }
}
class Ee extends dh {
  constructor(e, t, i, s) {
    super(), this._tree = e, this.from = t, this.index = i, this._parent = s;
  }
  get type() {
    return this._tree.type;
  }
  get name() {
    return this._tree.type.name;
  }
  get to() {
    return this.from + this._tree.length;
  }
  nextChild(e, t, i, s, r = 0) {
    for (let o = this; ; ) {
      for (let { children: l, positions: a } = o._tree, h = t > 0 ? l.length : -1; e != h; e += t) {
        let c = l[e], f = a[e] + o.from, u;
        if (!(!(r & X.EnterBracketed && c instanceof G && (u = Ci.get(c)) && !u.overlay && u.bracketed && i >= f && i <= f + c.length) && !uh(s, i, f, f + c.length))) {
          if (c instanceof mt) {
            if (r & X.ExcludeBuffers)
              continue;
            let d = c.findChild(0, c.buffer.length, t, i - f, s);
            if (d > -1)
              return new ut(new Vd(o, c, e, f), null, d);
          } else if (r & X.IncludeAnonymous || !c.type.isAnonymous || Wr(c)) {
            let d;
            if (!(r & X.IgnoreMounts) && (d = Ci.get(c)) && !d.overlay)
              return new Ee(d.tree, f, e, o);
            let p = new Ee(c, f, e, o);
            return r & X.IncludeAnonymous || !p.type.isAnonymous ? p : p.nextChild(t < 0 ? c.children.length - 1 : 0, t, i, s, r);
          }
        }
      }
      if (r & X.IncludeAnonymous || !o.type.isAnonymous || (o.index >= 0 ? e = o.index + t : e = t < 0 ? -1 : o._parent._tree.children.length, o = o._parent, !o))
        return null;
    }
  }
  get firstChild() {
    return this.nextChild(
      0,
      1,
      0,
      4
      /* Side.DontCare */
    );
  }
  get lastChild() {
    return this.nextChild(
      this._tree.children.length - 1,
      -1,
      0,
      4
      /* Side.DontCare */
    );
  }
  childAfter(e) {
    return this.nextChild(
      0,
      1,
      e,
      2
      /* Side.After */
    );
  }
  childBefore(e) {
    return this.nextChild(
      this._tree.children.length - 1,
      -1,
      e,
      -2
      /* Side.Before */
    );
  }
  prop(e) {
    return this._tree.prop(e);
  }
  enter(e, t, i = 0) {
    let s;
    if (!(i & X.IgnoreOverlays) && (s = Ci.get(this._tree)) && s.overlay) {
      let r = e - this.from, o = i & X.EnterBracketed && s.bracketed;
      for (let { from: l, to: a } of s.overlay)
        if ((t > 0 || o ? l <= r : l < r) && (t < 0 || o ? a >= r : a > r))
          return new Ee(s.tree, s.overlay[0].from + this.from, -1, this);
    }
    return this.nextChild(0, 1, e, t, i);
  }
  nextSignificantParent() {
    let e = this;
    for (; e.type.isAnonymous && e._parent; )
      e = e._parent;
    return e;
  }
  get parent() {
    return this._parent ? this._parent.nextSignificantParent() : null;
  }
  get nextSibling() {
    return this._parent && this.index >= 0 ? this._parent.nextChild(
      this.index + 1,
      1,
      0,
      4
      /* Side.DontCare */
    ) : null;
  }
  get prevSibling() {
    return this._parent && this.index >= 0 ? this._parent.nextChild(
      this.index - 1,
      -1,
      0,
      4
      /* Side.DontCare */
    ) : null;
  }
  get tree() {
    return this._tree;
  }
  toTree() {
    return this._tree;
  }
  /**
  @internal
  */
  toString() {
    return this._tree.toString();
  }
}
function cl(n, e, t, i) {
  let s = n.cursor(), r = [];
  if (!s.firstChild())
    return r;
  if (t != null) {
    for (let o = !1; !o; )
      if (o = s.type.is(t), !s.nextSibling())
        return r;
  }
  for (; ; ) {
    if (i != null && s.type.is(i))
      return r;
    if (s.type.is(e) && r.push(s.node), !s.nextSibling())
      return i == null ? r : [];
  }
}
function fr(n, e, t = e.length - 1) {
  for (let i = n; t >= 0; i = i.parent) {
    if (!i)
      return !1;
    if (!i.type.isAnonymous) {
      if (e[t] && e[t] != i.name)
        return !1;
      t--;
    }
  }
  return !0;
}
class Vd {
  constructor(e, t, i, s) {
    this.parent = e, this.buffer = t, this.index = i, this.start = s;
  }
}
class ut extends dh {
  get name() {
    return this.type.name;
  }
  get from() {
    return this.context.start + this.context.buffer.buffer[this.index + 1];
  }
  get to() {
    return this.context.start + this.context.buffer.buffer[this.index + 2];
  }
  constructor(e, t, i) {
    super(), this.context = e, this._parent = t, this.index = i, this.type = e.buffer.set.types[e.buffer.buffer[i]];
  }
  child(e, t, i) {
    let { buffer: s } = this.context, r = s.findChild(this.index + 4, s.buffer[this.index + 3], e, t - this.context.start, i);
    return r < 0 ? null : new ut(this.context, this, r);
  }
  get firstChild() {
    return this.child(
      1,
      0,
      4
      /* Side.DontCare */
    );
  }
  get lastChild() {
    return this.child(
      -1,
      0,
      4
      /* Side.DontCare */
    );
  }
  childAfter(e) {
    return this.child(
      1,
      e,
      2
      /* Side.After */
    );
  }
  childBefore(e) {
    return this.child(
      -1,
      e,
      -2
      /* Side.Before */
    );
  }
  prop(e) {
    return this.type.prop(e);
  }
  enter(e, t, i = 0) {
    if (i & X.ExcludeBuffers)
      return null;
    let { buffer: s } = this.context, r = s.findChild(this.index + 4, s.buffer[this.index + 3], t > 0 ? 1 : -1, e - this.context.start, t);
    return r < 0 ? null : new ut(this.context, this, r);
  }
  get parent() {
    return this._parent || this.context.parent.nextSignificantParent();
  }
  externalSibling(e) {
    return this._parent ? null : this.context.parent.nextChild(
      this.context.index + e,
      e,
      0,
      4
      /* Side.DontCare */
    );
  }
  get nextSibling() {
    let { buffer: e } = this.context, t = e.buffer[this.index + 3];
    return t < (this._parent ? e.buffer[this._parent.index + 3] : e.buffer.length) ? new ut(this.context, this._parent, t) : this.externalSibling(1);
  }
  get prevSibling() {
    let { buffer: e } = this.context, t = this._parent ? this._parent.index + 4 : 0;
    return this.index == t ? this.externalSibling(-1) : new ut(this.context, this._parent, e.findChild(
      t,
      this.index,
      -1,
      0,
      4
      /* Side.DontCare */
    ));
  }
  get tree() {
    return null;
  }
  toTree() {
    let e = [], t = [], { buffer: i } = this.context, s = this.index + 4, r = i.buffer[this.index + 3];
    if (r > s) {
      let o = i.buffer[this.index + 1];
      e.push(i.slice(s, r, o)), t.push(0);
    }
    return new G(this.type, e, t, this.to - this.from);
  }
  /**
  @internal
  */
  toString() {
    return this.context.buffer.childString(this.index);
  }
}
function ph(n) {
  if (!n.length)
    return null;
  let e = 0, t = n[0];
  for (let r = 1; r < n.length; r++) {
    let o = n[r];
    (o.from > t.from || o.to < t.to) && (t = o, e = r);
  }
  let i = t instanceof Ee && t.index < 0 ? null : t.parent, s = n.slice();
  return i ? s[e] = i : s.splice(e, 1), new Fd(s, t);
}
class Fd {
  constructor(e, t) {
    this.heads = e, this.node = t;
  }
  get next() {
    return ph(this.heads);
  }
}
function Wd(n, e, t) {
  let i = n.resolveInner(e, t), s = null;
  for (let r = i instanceof Ee ? i : i.context.parent; r; r = r.parent)
    if (r.index < 0) {
      let o = r.parent;
      (s || (s = [i])).push(o.resolve(e, t)), r = o;
    } else {
      let o = Ci.get(r.tree);
      if (o && o.overlay && o.overlay[0].from <= e && o.overlay[o.overlay.length - 1].to >= e) {
        let l = new Ee(o.tree, o.overlay[0].from + r.from, -1, r);
        (s || (s = [i])).push(Pi(l, e, t, !1));
      }
    }
  return s ? ph(s) : i;
}
class ur {
  /**
  Shorthand for `.type.name`.
  */
  get name() {
    return this.type.name;
  }
  /**
  @internal
  */
  constructor(e, t = 0) {
    if (this.buffer = null, this.stack = [], this.index = 0, this.bufferNode = null, this.mode = t & ~X.EnterBracketed, e instanceof Ee)
      this.yieldNode(e);
    else {
      this._tree = e.context.parent, this.buffer = e.context;
      for (let i = e._parent; i; i = i._parent)
        this.stack.unshift(i.index);
      this.bufferNode = e, this.yieldBuf(e.index);
    }
  }
  yieldNode(e) {
    return e ? (this._tree = e, this.type = e.type, this.from = e.from, this.to = e.to, !0) : !1;
  }
  yieldBuf(e, t) {
    this.index = e;
    let { start: i, buffer: s } = this.buffer;
    return this.type = t || s.set.types[s.buffer[e]], this.from = i + s.buffer[e + 1], this.to = i + s.buffer[e + 2], !0;
  }
  /**
  @internal
  */
  yield(e) {
    return e ? e instanceof Ee ? (this.buffer = null, this.yieldNode(e)) : (this.buffer = e.context, this.yieldBuf(e.index, e.type)) : !1;
  }
  /**
  @internal
  */
  toString() {
    return this.buffer ? this.buffer.buffer.childString(this.index) : this._tree.toString();
  }
  /**
  @internal
  */
  enterChild(e, t, i) {
    if (!this.buffer)
      return this.yield(this._tree.nextChild(e < 0 ? this._tree._tree.children.length - 1 : 0, e, t, i, this.mode));
    let { buffer: s } = this.buffer, r = s.findChild(this.index + 4, s.buffer[this.index + 3], e, t - this.buffer.start, i);
    return r < 0 ? !1 : (this.stack.push(this.index), this.yieldBuf(r));
  }
  /**
  Move the cursor to this node's first child. When this returns
  false, the node has no child, and the cursor has not been moved.
  */
  firstChild() {
    return this.enterChild(
      1,
      0,
      4
      /* Side.DontCare */
    );
  }
  /**
  Move the cursor to this node's last child.
  */
  lastChild() {
    return this.enterChild(
      -1,
      0,
      4
      /* Side.DontCare */
    );
  }
  /**
  Move the cursor to the first child that ends after `pos`.
  */
  childAfter(e) {
    return this.enterChild(
      1,
      e,
      2
      /* Side.After */
    );
  }
  /**
  Move to the last child that starts before `pos`.
  */
  childBefore(e) {
    return this.enterChild(
      -1,
      e,
      -2
      /* Side.Before */
    );
  }
  /**
  Move the cursor to the child around `pos`. If side is -1 the
  child may end at that position, when 1 it may start there. This
  will also enter [overlaid](#common.MountedTree.overlay)
  [mounted](#common.NodeProp^mounted) trees unless `overlays` is
  set to false.
  */
  enter(e, t, i = this.mode) {
    return this.buffer ? i & X.ExcludeBuffers ? !1 : this.enterChild(1, e, t) : this.yield(this._tree.enter(e, t, i));
  }
  /**
  Move to the node's parent node, if this isn't the top node.
  */
  parent() {
    if (!this.buffer)
      return this.yieldNode(this.mode & X.IncludeAnonymous ? this._tree._parent : this._tree.parent);
    if (this.stack.length)
      return this.yieldBuf(this.stack.pop());
    let e = this.mode & X.IncludeAnonymous ? this.buffer.parent : this.buffer.parent.nextSignificantParent();
    return this.buffer = null, this.yieldNode(e);
  }
  /**
  @internal
  */
  sibling(e) {
    if (!this.buffer)
      return this._tree._parent ? this.yield(this._tree.index < 0 ? null : this._tree._parent.nextChild(this._tree.index + e, e, 0, 4, this.mode)) : !1;
    let { buffer: t } = this.buffer, i = this.stack.length - 1;
    if (e < 0) {
      let s = i < 0 ? 0 : this.stack[i] + 4;
      if (this.index != s)
        return this.yieldBuf(t.findChild(
          s,
          this.index,
          -1,
          0,
          4
          /* Side.DontCare */
        ));
    } else {
      let s = t.buffer[this.index + 3];
      if (s < (i < 0 ? t.buffer.length : t.buffer[this.stack[i] + 3]))
        return this.yieldBuf(s);
    }
    return i < 0 ? this.yield(this.buffer.parent.nextChild(this.buffer.index + e, e, 0, 4, this.mode)) : !1;
  }
  /**
  Move to this node's next sibling, if any.
  */
  nextSibling() {
    return this.sibling(1);
  }
  /**
  Move to this node's previous sibling, if any.
  */
  prevSibling() {
    return this.sibling(-1);
  }
  atLastNode(e) {
    let t, i, { buffer: s } = this;
    if (s) {
      if (e > 0) {
        if (this.index < s.buffer.buffer.length)
          return !1;
      } else
        for (let r = 0; r < this.index; r++)
          if (s.buffer.buffer[r + 3] < this.index)
            return !1;
      ({ index: t, parent: i } = s);
    } else
      ({ index: t, _parent: i } = this._tree);
    for (; i; { index: t, _parent: i } = i)
      if (t > -1)
        for (let r = t + e, o = e < 0 ? -1 : i._tree.children.length; r != o; r += e) {
          let l = i._tree.children[r];
          if (this.mode & X.IncludeAnonymous || l instanceof mt || !l.type.isAnonymous || Wr(l))
            return !1;
        }
    return !0;
  }
  move(e, t) {
    if (t && this.enterChild(
      e,
      0,
      4
      /* Side.DontCare */
    ))
      return !0;
    for (; ; ) {
      if (this.sibling(e))
        return !0;
      if (this.atLastNode(e) || !this.parent())
        return !1;
    }
  }
  /**
  Move to the next node in a
  [pre-order](https://en.wikipedia.org/wiki/Tree_traversal#Pre-order,_NLR)
  traversal, going from a node to its first child or, if the
  current node is empty or `enter` is false, its next sibling or
  the next sibling of the first parent node that has one.
  */
  next(e = !0) {
    return this.move(1, e);
  }
  /**
  Move to the next node in a last-to-first pre-order traversal. A
  node is followed by its last child or, if it has none, its
  previous sibling or the previous sibling of the first parent
  node that has one.
  */
  prev(e = !0) {
    return this.move(-1, e);
  }
  /**
  Move the cursor to the innermost node that covers `pos`. If
  `side` is -1, it will enter nodes that end at `pos`. If it is 1,
  it will enter nodes that start at `pos`.
  */
  moveTo(e, t = 0) {
    for (; (this.from == this.to || (t < 1 ? this.from >= e : this.from > e) || (t > -1 ? this.to <= e : this.to < e)) && this.parent(); )
      ;
    for (; this.enterChild(1, e, t); )
      ;
    return this;
  }
  /**
  Get a [syntax node](#common.SyntaxNode) at the cursor's current
  position.
  */
  get node() {
    if (!this.buffer)
      return this._tree;
    let e = this.bufferNode, t = null, i = 0;
    if (e && e.context == this.buffer)
      e: for (let s = this.index, r = this.stack.length; r >= 0; ) {
        for (let o = e; o; o = o._parent)
          if (o.index == s) {
            if (s == this.index)
              return o;
            t = o, i = r + 1;
            break e;
          }
        s = this.stack[--r];
      }
    for (let s = i; s < this.stack.length; s++)
      t = new ut(this.buffer, t, this.stack[s]);
    return this.bufferNode = new ut(this.buffer, t, this.index);
  }
  /**
  Get the [tree](#common.Tree) that represents the current node, if
  any. Will return null when the node is in a [tree
  buffer](#common.TreeBuffer).
  */
  get tree() {
    return this.buffer ? null : this._tree._tree;
  }
  /**
  Iterate over the current node and all its descendants, calling
  `enter` when entering a node and `leave`, if given, when leaving
  one. When `enter` returns `false`, any children of that node are
  skipped, and `leave` isn't called for it.
  */
  iterate(e, t) {
    for (let i = 0; ; ) {
      let s = !1;
      if (this.type.isAnonymous || e(this) !== !1) {
        if (this.firstChild()) {
          i++;
          continue;
        }
        this.type.isAnonymous || (s = !0);
      }
      for (; ; ) {
        if (s && t && t(this), s = this.type.isAnonymous, !i)
          return;
        if (this.nextSibling())
          break;
        this.parent(), i--, s = !0;
      }
    }
  }
  /**
  Test whether the current node matches a given context—a sequence
  of direct parent node names. Empty strings in the context array
  are treated as wildcards.
  */
  matchContext(e) {
    if (!this.buffer)
      return fr(this.node.parent, e);
    let { buffer: t } = this.buffer, { types: i } = t.set;
    for (let s = e.length - 1, r = this.stack.length - 1; s >= 0; r--) {
      if (r < 0)
        return fr(this._tree, e, s);
      let o = i[t.buffer[this.stack[r]]];
      if (!o.isAnonymous) {
        if (e[s] && e[s] != o.name)
          return !1;
        s--;
      }
    }
    return !0;
  }
}
function Wr(n) {
  return n.children.some((e) => e instanceof mt || !e.type.isAnonymous || Wr(e));
}
function _d(n) {
  var e;
  let { buffer: t, nodeSet: i, maxBufferLength: s = $d, reused: r = [], minRepeatType: o = i.types.length } = n, l = Array.isArray(t) ? new Fr(t, t.length) : t, a = i.types, h = 0, c = 0;
  function f(k, S, A, L, P, _) {
    let { id: R, start: D, end: F, size: H } = l, U = c, ce = h;
    if (H < 0)
      if (l.next(), H == -1) {
        let Qe = r[R];
        A.push(Qe), L.push(D - k);
        return;
      } else if (H == -3) {
        h = R;
        return;
      } else if (H == -4) {
        c = R;
        return;
      } else
        throw new RangeError(`Unrecognized record size: ${H}`);
    let xe = a[R], Fe, ie, Ce = D - k;
    if (F - D <= s && (ie = m(l.pos - S, P))) {
      let Qe = new Uint16Array(ie.size - ie.skip), Me = l.pos - ie.size, We = Qe.length;
      for (; l.pos > Me; )
        We = b(ie.start, Qe, We);
      Fe = new mt(Qe, F - ie.start, i), Ce = ie.start - k;
    } else {
      let Qe = l.pos - H;
      l.next();
      let Me = [], We = [], vt = R >= o ? R : -1, Ht = 0, Ji = F;
      for (; l.pos > Qe; )
        vt >= 0 && l.id == vt && l.size >= 0 ? (l.end <= Ji - s && (p(Me, We, D, Ht, l.end, Ji, vt, U, ce), Ht = Me.length, Ji = l.end), l.next()) : _ > 2500 ? u(D, Qe, Me, We) : f(D, Qe, Me, We, vt, _ + 1);
      if (vt >= 0 && Ht > 0 && Ht < Me.length && p(Me, We, D, Ht, D, Ji, vt, U, ce), Me.reverse(), We.reverse(), vt > -1 && Ht > 0) {
        let ao = d(xe, ce);
        Fe = _r(xe, Me, We, 0, Me.length, 0, F - D, ao, ao);
      } else
        Fe = g(xe, Me, We, F - D, U - F, ce);
    }
    A.push(Fe), L.push(Ce);
  }
  function u(k, S, A, L) {
    let P = [], _ = 0, R = -1;
    for (; l.pos > S; ) {
      let { id: D, start: F, end: H, size: U } = l;
      if (U > 4)
        l.next();
      else {
        if (R > -1 && F < R)
          break;
        R < 0 && (R = H - s), P.push(D, F, H), _++, l.next();
      }
    }
    if (_) {
      let D = new Uint16Array(_ * 4), F = P[P.length - 2];
      for (let H = P.length - 3, U = 0; H >= 0; H -= 3)
        D[U++] = P[H], D[U++] = P[H + 1] - F, D[U++] = P[H + 2] - F, D[U++] = U;
      A.push(new mt(D, P[2] - F, i)), L.push(F - k);
    }
  }
  function d(k, S) {
    return (A, L, P) => {
      let _ = 0, R = A.length - 1, D, F;
      if (R >= 0 && (D = A[R]) instanceof G) {
        if (!R && D.type == k && D.length == P)
          return D;
        (F = D.prop(I.lookAhead)) && (_ = L[R] + D.length + F);
      }
      return g(k, A, L, P, _, S);
    };
  }
  function p(k, S, A, L, P, _, R, D, F) {
    let H = [], U = [];
    for (; k.length > L; )
      H.push(k.pop()), U.push(S.pop() + A - P);
    k.push(g(i.types[R], H, U, _ - P, D - _, F)), S.push(P - A);
  }
  function g(k, S, A, L, P, _, R) {
    if (_) {
      let D = [I.contextHash, _];
      R = R ? [D].concat(R) : [D];
    }
    if (P > 25) {
      let D = [I.lookAhead, P];
      R = R ? [D].concat(R) : [D];
    }
    return new G(k, S, A, L, R);
  }
  function m(k, S) {
    let A = l.fork(), L = 0, P = 0, _ = 0, R = A.end - s, D = { size: 0, start: 0, skip: 0 };
    e: for (let F = A.pos - k; A.pos > F; ) {
      let H = A.size;
      if (A.id == S && H >= 0) {
        D.size = L, D.start = P, D.skip = _, _ += 4, L += 4, A.next();
        continue;
      }
      let U = A.pos - H;
      if (H < 0 || U < F || A.start < R)
        break;
      let ce = A.id >= o ? 4 : 0, xe = A.start;
      for (A.next(); A.pos > U; ) {
        if (A.size < 0)
          if (A.size == -3 || A.size == -4)
            ce += 4;
          else
            break e;
        else A.id >= o && (ce += 4);
        A.next();
      }
      P = xe, L += H, _ += ce;
    }
    return (S < 0 || L == k) && (D.size = L, D.start = P, D.skip = _), D.size > 4 ? D : void 0;
  }
  function b(k, S, A) {
    let { id: L, start: P, end: _, size: R } = l;
    if (l.next(), R >= 0 && L < o) {
      let D = A;
      if (R > 4) {
        let F = l.pos - (R - 4);
        for (; l.pos > F; )
          A = b(k, S, A);
      }
      S[--A] = D, S[--A] = _ - k, S[--A] = P - k, S[--A] = L;
    } else R == -3 ? h = L : R == -4 && (c = L);
    return A;
  }
  let v = [], w = [];
  for (; l.pos > 0; )
    f(n.start || 0, n.bufferStart || 0, v, w, -1, 0);
  let O = (e = n.length) !== null && e !== void 0 ? e : v.length ? w[0] + v[0].length : 0;
  return new G(a[n.topID], v.reverse(), w.reverse(), O);
}
const fl = /* @__PURE__ */ new WeakMap();
function Sn(n, e) {
  if (!n.isAnonymous || e instanceof mt || e.type != n)
    return 1;
  let t = fl.get(e);
  if (t == null) {
    t = 1;
    for (let i of e.children) {
      if (i.type != n || !(i instanceof G)) {
        t = 1;
        break;
      }
      t += Sn(n, i);
    }
    fl.set(e, t);
  }
  return t;
}
function _r(n, e, t, i, s, r, o, l, a) {
  let h = 0;
  for (let p = i; p < s; p++)
    h += Sn(n, e[p]);
  let c = Math.ceil(
    h * 1.5 / 8
    /* Balance.BranchFactor */
  ), f = [], u = [];
  function d(p, g, m, b, v) {
    for (let w = m; w < b; ) {
      let O = w, k = g[w], S = Sn(n, p[w]);
      for (w++; w < b; w++) {
        let A = Sn(n, p[w]);
        if (S + A >= c)
          break;
        S += A;
      }
      if (w == O + 1) {
        if (S > c) {
          let A = p[O];
          d(A.children, A.positions, 0, A.children.length, g[O] + v);
          continue;
        }
        f.push(p[O]);
      } else {
        let A = g[w - 1] + p[w - 1].length - k;
        f.push(_r(n, p, g, O, w, k, A, null, a));
      }
      u.push(k + v - r);
    }
  }
  return d(e, t, i, s, 0), (l || a)(f, u, o);
}
class Et {
  /**
  Construct a tree fragment. You'll usually want to use
  [`addTree`](#common.TreeFragment^addTree) and
  [`applyChanges`](#common.TreeFragment^applyChanges) instead of
  calling this directly.
  */
  constructor(e, t, i, s, r = !1, o = !1) {
    this.from = e, this.to = t, this.tree = i, this.offset = s, this.open = (r ? 1 : 0) | (o ? 2 : 0);
  }
  /**
  Whether the start of the fragment represents the start of a
  parse, or the end of a change. (In the second case, it may not
  be safe to reuse some nodes at the start, depending on the
  parsing algorithm.)
  */
  get openStart() {
    return (this.open & 1) > 0;
  }
  /**
  Whether the end of the fragment represents the end of a
  full-document parse, or the start of a change.
  */
  get openEnd() {
    return (this.open & 2) > 0;
  }
  /**
  Create a set of fragments from a freshly parsed tree, or update
  an existing set of fragments by replacing the ones that overlap
  with a tree with content from the new tree. When `partial` is
  true, the parse is treated as incomplete, and the resulting
  fragment has [`openEnd`](#common.TreeFragment.openEnd) set to
  true.
  */
  static addTree(e, t = [], i = !1) {
    let s = [new Et(0, e.length, e, 0, !1, i)];
    for (let r of t)
      r.to > e.length && s.push(r);
    return s;
  }
  /**
  Apply a set of edits to an array of fragments, removing or
  splitting fragments as necessary to remove edited ranges, and
  adjusting offsets for fragments that moved.
  */
  static applyChanges(e, t, i = 128) {
    if (!t.length)
      return e;
    let s = [], r = 1, o = e.length ? e[0] : null;
    for (let l = 0, a = 0, h = 0; ; l++) {
      let c = l < t.length ? t[l] : null, f = c ? c.fromA : 1e9;
      if (f - a >= i)
        for (; o && o.from < f; ) {
          let u = o;
          if (a >= u.from || f <= u.to || h) {
            let d = Math.max(u.from, a) - h, p = Math.min(u.to, f) - h;
            u = d >= p ? null : new Et(d, p, u.tree, u.offset + h, l > 0, !!c);
          }
          if (u && s.push(u), o.to > f)
            break;
          o = r < e.length ? e[r++] : null;
        }
      if (!c)
        break;
      a = c.toA, h = c.toA - c.toB;
    }
    return s;
  }
}
class gh {
  /**
  Start a parse, returning a [partial parse](#common.PartialParse)
  object. [`fragments`](#common.TreeFragment) can be passed in to
  make the parse incremental.
  
  By default, the entire input is parsed. You can pass `ranges`,
  which should be a sorted array of non-empty, non-overlapping
  ranges, to parse only those ranges. The tree returned in that
  case will start at `ranges[0].from`.
  */
  startParse(e, t, i) {
    return typeof e == "string" && (e = new zd(e)), i = i ? i.length ? i.map((s) => new ks(s.from, s.to)) : [new ks(0, 0)] : [new ks(0, e.length)], this.createParse(e, t || [], i);
  }
  /**
  Run a full parse, returning the resulting tree.
  */
  parse(e, t, i) {
    let s = this.startParse(e, t, i);
    for (; ; ) {
      let r = s.advance();
      if (r)
        return r;
    }
  }
}
class zd {
  constructor(e) {
    this.string = e;
  }
  get length() {
    return this.string.length;
  }
  chunk(e) {
    return this.string.slice(e);
  }
  get lineChunks() {
    return !1;
  }
  read(e, t) {
    return this.string.slice(e, t);
  }
}
new I({ perNode: !0 });
let Ud = 0;
class Te {
  /**
  @internal
  */
  constructor(e, t, i, s) {
    this.name = e, this.set = t, this.base = i, this.modified = s, this.id = Ud++;
  }
  toString() {
    let { name: e } = this;
    for (let t of this.modified)
      t.name && (e = `${t.name}(${e})`);
    return e;
  }
  static define(e, t) {
    let i = typeof e == "string" ? e : "?";
    if (e instanceof Te && (t = e), t?.base)
      throw new Error("Can not derive from a modified tag");
    let s = new Te(i, [], null, []);
    if (s.set.push(s), t)
      for (let r of t.set)
        s.set.push(r);
    return s;
  }
  /**
  Define a tag _modifier_, which is a function that, given a tag,
  will return a tag that is a subtag of the original. Applying the
  same modifier to a twice tag will return the same value (`m1(t1)
  == m1(t1)`) and applying multiple modifiers will, regardless or
  order, produce the same tag (`m1(m2(t1)) == m2(m1(t1))`).
  
  When multiple modifiers are applied to a given base tag, each
  smaller set of modifiers is registered as a parent, so that for
  example `m1(m2(m3(t1)))` is a subtype of `m1(m2(t1))`,
  `m1(m3(t1)`, and so on.
  */
  static defineModifier(e) {
    let t = new Hn(e);
    return (i) => i.modified.indexOf(t) > -1 ? i : Hn.get(i.base || i, i.modified.concat(t).sort((s, r) => s.id - r.id));
  }
}
let jd = 0;
class Hn {
  constructor(e) {
    this.name = e, this.instances = [], this.id = jd++;
  }
  static get(e, t) {
    if (!t.length)
      return e;
    let i = t[0].instances.find((l) => l.base == e && qd(t, l.modified));
    if (i)
      return i;
    let s = [], r = new Te(e.name, s, e, t);
    for (let l of t)
      l.instances.push(r);
    let o = Kd(t);
    for (let l of e.set)
      if (!l.modified.length)
        for (let a of o)
          s.push(Hn.get(l, a));
    return r;
  }
}
function qd(n, e) {
  return n.length == e.length && n.every((t, i) => t == e[i]);
}
function Kd(n) {
  let e = [[]];
  for (let t = 0; t < n.length; t++)
    for (let i = 0, s = e.length; i < s; i++)
      e.push(e[i].concat(n[t]));
  return e.sort((t, i) => i.length - t.length);
}
function Gd(n) {
  let e = /* @__PURE__ */ Object.create(null);
  for (let t in n) {
    let i = n[t];
    Array.isArray(i) || (i = [i]);
    for (let s of t.split(" "))
      if (s) {
        let r = [], o = 2, l = s;
        for (let f = 0; ; ) {
          if (l == "..." && f > 0 && f + 3 == s.length) {
            o = 1;
            break;
          }
          let u = /^"(?:[^"\\]|\\.)*?"|[^\/!]+/.exec(l);
          if (!u)
            throw new RangeError("Invalid path: " + s);
          if (r.push(u[0] == "*" ? "" : u[0][0] == '"' ? JSON.parse(u[0]) : u[0]), f += u[0].length, f == s.length)
            break;
          let d = s[f++];
          if (f == s.length && d == "!") {
            o = 0;
            break;
          }
          if (d != "/")
            throw new RangeError("Invalid path: " + s);
          l = s.slice(f);
        }
        let a = r.length - 1, h = r[a];
        if (!h)
          throw new RangeError("Invalid path: " + s);
        let c = new Ii(i, o, a > 0 ? r.slice(0, a) : null);
        e[h] = c.sort(e[h]);
      }
  }
  return mh.add(e);
}
const mh = new I({
  combine(n, e) {
    let t, i, s;
    for (; n || e; ) {
      if (!n || e && n.depth >= e.depth ? (s = e, e = e.next) : (s = n, n = n.next), t && t.mode == s.mode && !s.context && !t.context)
        continue;
      let r = new Ii(s.tags, s.mode, s.context);
      t ? t.next = r : i = r, t = r;
    }
    return i;
  }
});
class Ii {
  constructor(e, t, i, s) {
    this.tags = e, this.mode = t, this.context = i, this.next = s;
  }
  get opaque() {
    return this.mode == 0;
  }
  get inherit() {
    return this.mode == 1;
  }
  sort(e) {
    return !e || e.depth < this.depth ? (this.next = e, this) : (e.next = this.sort(e.next), e);
  }
  get depth() {
    return this.context ? this.context.length : 0;
  }
}
Ii.empty = new Ii([], 2, null);
function bh(n, e) {
  let t = /* @__PURE__ */ Object.create(null);
  for (let r of n)
    if (!Array.isArray(r.tag))
      t[r.tag.id] = r.class;
    else
      for (let o of r.tag)
        t[o.id] = r.class;
  let { scope: i, all: s = null } = e || {};
  return {
    style: (r) => {
      let o = s;
      for (let l of r)
        for (let a of l.set) {
          let h = t[a.id];
          if (h) {
            o = o ? o + " " + h : h;
            break;
          }
        }
      return o;
    },
    scope: i
  };
}
function Jd(n, e) {
  let t = null;
  for (let i of n) {
    let s = i.style(e);
    s && (t = t ? t + " " + s : s);
  }
  return t;
}
function Yd(n, e, t, i = 0, s = n.length) {
  let r = new Xd(i, Array.isArray(e) ? e : [e], t);
  r.highlightRange(n.cursor(), i, s, "", r.highlighters), r.flush(s);
}
class Xd {
  constructor(e, t, i) {
    this.at = e, this.highlighters = t, this.span = i, this.class = "";
  }
  startSpan(e, t) {
    t != this.class && (this.flush(e), e > this.at && (this.at = e), this.class = t);
  }
  flush(e) {
    e > this.at && this.class && this.span(this.at, e, this.class);
  }
  highlightRange(e, t, i, s, r) {
    let { type: o, from: l, to: a } = e;
    if (l >= i || a <= t)
      return;
    o.isTop && (r = this.highlighters.filter((d) => !d.scope || d.scope(o)));
    let h = s, c = Zd(e) || Ii.empty, f = Jd(r, c.tags);
    if (f && (h && (h += " "), h += f, c.mode == 1 && (s += (s ? " " : "") + f)), this.startSpan(Math.max(t, l), h), c.opaque)
      return;
    let u = e.tree && e.tree.prop(I.mounted);
    if (u && u.overlay) {
      let d = e.node.enter(u.overlay[0].from + l, 1), p = this.highlighters.filter((m) => !m.scope || m.scope(u.tree.type)), g = e.firstChild();
      for (let m = 0, b = l; ; m++) {
        let v = m < u.overlay.length ? u.overlay[m] : null, w = v ? v.from + l : a, O = Math.max(t, b), k = Math.min(i, w);
        if (O < k && g)
          for (; e.from < k && (this.highlightRange(e, O, k, s, r), this.startSpan(Math.min(k, e.to), h), !(e.to >= w || !e.nextSibling())); )
            ;
        if (!v || w > i)
          break;
        b = v.to + l, b > t && (this.highlightRange(d.cursor(), Math.max(t, v.from + l), Math.min(i, b), "", p), this.startSpan(Math.min(i, b), h));
      }
      g && e.parent();
    } else if (e.firstChild()) {
      u && (s = "");
      do
        if (!(e.to <= t)) {
          if (e.from >= i)
            break;
          this.highlightRange(e, t, i, s, r), this.startSpan(Math.min(i, e.to), h);
        }
      while (e.nextSibling());
      e.parent();
    }
  }
}
function Zd(n) {
  let e = n.type.prop(mh);
  for (; e && e.context && !n.matchContext(e.context); )
    e = e.next;
  return e || null;
}
const x = Te.define, un = x(), lt = x(), ul = x(lt), dl = x(lt), at = x(), dn = x(at), Ss = x(at), je = x(), xt = x(je), ze = x(), Ue = x(), dr = x(), fi = x(dr), pn = x(), E = {
  /**
  A comment.
  */
  comment: un,
  /**
  A line [comment](#highlight.tags.comment).
  */
  lineComment: x(un),
  /**
  A block [comment](#highlight.tags.comment).
  */
  blockComment: x(un),
  /**
  A documentation [comment](#highlight.tags.comment).
  */
  docComment: x(un),
  /**
  Any kind of identifier.
  */
  name: lt,
  /**
  The [name](#highlight.tags.name) of a variable.
  */
  variableName: x(lt),
  /**
  A type [name](#highlight.tags.name).
  */
  typeName: ul,
  /**
  A tag name (subtag of [`typeName`](#highlight.tags.typeName)).
  */
  tagName: x(ul),
  /**
  A property or field [name](#highlight.tags.name).
  */
  propertyName: dl,
  /**
  An attribute name (subtag of [`propertyName`](#highlight.tags.propertyName)).
  */
  attributeName: x(dl),
  /**
  The [name](#highlight.tags.name) of a class.
  */
  className: x(lt),
  /**
  A label [name](#highlight.tags.name).
  */
  labelName: x(lt),
  /**
  A namespace [name](#highlight.tags.name).
  */
  namespace: x(lt),
  /**
  The [name](#highlight.tags.name) of a macro.
  */
  macroName: x(lt),
  /**
  A literal value.
  */
  literal: at,
  /**
  A string [literal](#highlight.tags.literal).
  */
  string: dn,
  /**
  A documentation [string](#highlight.tags.string).
  */
  docString: x(dn),
  /**
  A character literal (subtag of [string](#highlight.tags.string)).
  */
  character: x(dn),
  /**
  An attribute value (subtag of [string](#highlight.tags.string)).
  */
  attributeValue: x(dn),
  /**
  A number [literal](#highlight.tags.literal).
  */
  number: Ss,
  /**
  An integer [number](#highlight.tags.number) literal.
  */
  integer: x(Ss),
  /**
  A floating-point [number](#highlight.tags.number) literal.
  */
  float: x(Ss),
  /**
  A boolean [literal](#highlight.tags.literal).
  */
  bool: x(at),
  /**
  Regular expression [literal](#highlight.tags.literal).
  */
  regexp: x(at),
  /**
  An escape [literal](#highlight.tags.literal), for example a
  backslash escape in a string.
  */
  escape: x(at),
  /**
  A color [literal](#highlight.tags.literal).
  */
  color: x(at),
  /**
  A URL [literal](#highlight.tags.literal).
  */
  url: x(at),
  /**
  A language keyword.
  */
  keyword: ze,
  /**
  The [keyword](#highlight.tags.keyword) for the self or this
  object.
  */
  self: x(ze),
  /**
  The [keyword](#highlight.tags.keyword) for null.
  */
  null: x(ze),
  /**
  A [keyword](#highlight.tags.keyword) denoting some atomic value.
  */
  atom: x(ze),
  /**
  A [keyword](#highlight.tags.keyword) that represents a unit.
  */
  unit: x(ze),
  /**
  A modifier [keyword](#highlight.tags.keyword).
  */
  modifier: x(ze),
  /**
  A [keyword](#highlight.tags.keyword) that acts as an operator.
  */
  operatorKeyword: x(ze),
  /**
  A control-flow related [keyword](#highlight.tags.keyword).
  */
  controlKeyword: x(ze),
  /**
  A [keyword](#highlight.tags.keyword) that defines something.
  */
  definitionKeyword: x(ze),
  /**
  A [keyword](#highlight.tags.keyword) related to defining or
  interfacing with modules.
  */
  moduleKeyword: x(ze),
  /**
  An operator.
  */
  operator: Ue,
  /**
  An [operator](#highlight.tags.operator) that dereferences something.
  */
  derefOperator: x(Ue),
  /**
  Arithmetic-related [operator](#highlight.tags.operator).
  */
  arithmeticOperator: x(Ue),
  /**
  Logical [operator](#highlight.tags.operator).
  */
  logicOperator: x(Ue),
  /**
  Bit [operator](#highlight.tags.operator).
  */
  bitwiseOperator: x(Ue),
  /**
  Comparison [operator](#highlight.tags.operator).
  */
  compareOperator: x(Ue),
  /**
  [Operator](#highlight.tags.operator) that updates its operand.
  */
  updateOperator: x(Ue),
  /**
  [Operator](#highlight.tags.operator) that defines something.
  */
  definitionOperator: x(Ue),
  /**
  Type-related [operator](#highlight.tags.operator).
  */
  typeOperator: x(Ue),
  /**
  Control-flow [operator](#highlight.tags.operator).
  */
  controlOperator: x(Ue),
  /**
  Program or markup punctuation.
  */
  punctuation: dr,
  /**
  [Punctuation](#highlight.tags.punctuation) that separates
  things.
  */
  separator: x(dr),
  /**
  Bracket-style [punctuation](#highlight.tags.punctuation).
  */
  bracket: fi,
  /**
  Angle [brackets](#highlight.tags.bracket) (usually `<` and `>`
  tokens).
  */
  angleBracket: x(fi),
  /**
  Square [brackets](#highlight.tags.bracket) (usually `[` and `]`
  tokens).
  */
  squareBracket: x(fi),
  /**
  Parentheses (usually `(` and `)` tokens). Subtag of
  [bracket](#highlight.tags.bracket).
  */
  paren: x(fi),
  /**
  Braces (usually `{` and `}` tokens). Subtag of
  [bracket](#highlight.tags.bracket).
  */
  brace: x(fi),
  /**
  Content, for example plain text in XML or markup documents.
  */
  content: je,
  /**
  [Content](#highlight.tags.content) that represents a heading.
  */
  heading: xt,
  /**
  A level 1 [heading](#highlight.tags.heading).
  */
  heading1: x(xt),
  /**
  A level 2 [heading](#highlight.tags.heading).
  */
  heading2: x(xt),
  /**
  A level 3 [heading](#highlight.tags.heading).
  */
  heading3: x(xt),
  /**
  A level 4 [heading](#highlight.tags.heading).
  */
  heading4: x(xt),
  /**
  A level 5 [heading](#highlight.tags.heading).
  */
  heading5: x(xt),
  /**
  A level 6 [heading](#highlight.tags.heading).
  */
  heading6: x(xt),
  /**
  A prose [content](#highlight.tags.content) separator (such as a horizontal rule).
  */
  contentSeparator: x(je),
  /**
  [Content](#highlight.tags.content) that represents a list.
  */
  list: x(je),
  /**
  [Content](#highlight.tags.content) that represents a quote.
  */
  quote: x(je),
  /**
  [Content](#highlight.tags.content) that is emphasized.
  */
  emphasis: x(je),
  /**
  [Content](#highlight.tags.content) that is styled strong.
  */
  strong: x(je),
  /**
  [Content](#highlight.tags.content) that is part of a link.
  */
  link: x(je),
  /**
  [Content](#highlight.tags.content) that is styled as code or
  monospace.
  */
  monospace: x(je),
  /**
  [Content](#highlight.tags.content) that has a strike-through
  style.
  */
  strikethrough: x(je),
  /**
  Inserted text in a change-tracking format.
  */
  inserted: x(),
  /**
  Deleted text.
  */
  deleted: x(),
  /**
  Changed text.
  */
  changed: x(),
  /**
  An invalid or unsyntactic element.
  */
  invalid: x(),
  /**
  Metadata or meta-instruction.
  */
  meta: pn,
  /**
  [Metadata](#highlight.tags.meta) that applies to the entire
  document.
  */
  documentMeta: x(pn),
  /**
  [Metadata](#highlight.tags.meta) that annotates or adds
  attributes to a given syntactic element.
  */
  annotation: x(pn),
  /**
  Processing instruction or preprocessor directive. Subtag of
  [meta](#highlight.tags.meta).
  */
  processingInstruction: x(pn),
  /**
  [Modifier](#highlight.Tag^defineModifier) that indicates that a
  given element is being defined. Expected to be used with the
  various [name](#highlight.tags.name) tags.
  */
  definition: Te.defineModifier("definition"),
  /**
  [Modifier](#highlight.Tag^defineModifier) that indicates that
  something is constant. Mostly expected to be used with
  [variable names](#highlight.tags.variableName).
  */
  constant: Te.defineModifier("constant"),
  /**
  [Modifier](#highlight.Tag^defineModifier) used to indicate that
  a [variable](#highlight.tags.variableName) or [property
  name](#highlight.tags.propertyName) is being called or defined
  as a function.
  */
  function: Te.defineModifier("function"),
  /**
  [Modifier](#highlight.Tag^defineModifier) that can be applied to
  [names](#highlight.tags.name) to indicate that they belong to
  the language's standard environment.
  */
  standard: Te.defineModifier("standard"),
  /**
  [Modifier](#highlight.Tag^defineModifier) that indicates a given
  [names](#highlight.tags.name) is local to some scope.
  */
  local: Te.defineModifier("local"),
  /**
  A generic variant [modifier](#highlight.Tag^defineModifier) that
  can be used to tag language-specific alternative variants of
  some common tag. It is recommended for themes to define special
  forms of at least the [string](#highlight.tags.string) and
  [variable name](#highlight.tags.variableName) tags, since those
  come up a lot.
  */
  special: Te.defineModifier("special")
};
for (let n in E) {
  let e = E[n];
  e instanceof Te && (e.name = n);
}
bh([
  { tag: E.link, class: "tok-link" },
  { tag: E.heading, class: "tok-heading" },
  { tag: E.emphasis, class: "tok-emphasis" },
  { tag: E.strong, class: "tok-strong" },
  { tag: E.keyword, class: "tok-keyword" },
  { tag: E.atom, class: "tok-atom" },
  { tag: E.bool, class: "tok-bool" },
  { tag: E.url, class: "tok-url" },
  { tag: E.labelName, class: "tok-labelName" },
  { tag: E.inserted, class: "tok-inserted" },
  { tag: E.deleted, class: "tok-deleted" },
  { tag: E.literal, class: "tok-literal" },
  { tag: E.string, class: "tok-string" },
  { tag: E.number, class: "tok-number" },
  { tag: [E.regexp, E.escape, E.special(E.string)], class: "tok-string2" },
  { tag: E.variableName, class: "tok-variableName" },
  { tag: E.local(E.variableName), class: "tok-variableName tok-local" },
  { tag: E.definition(E.variableName), class: "tok-variableName tok-definition" },
  { tag: E.special(E.variableName), class: "tok-variableName2" },
  { tag: E.definition(E.propertyName), class: "tok-propertyName tok-definition" },
  { tag: E.typeName, class: "tok-typeName" },
  { tag: E.namespace, class: "tok-namespace" },
  { tag: E.className, class: "tok-className" },
  { tag: E.macroName, class: "tok-macroName" },
  { tag: E.propertyName, class: "tok-propertyName" },
  { tag: E.operator, class: "tok-operator" },
  { tag: E.comment, class: "tok-comment" },
  { tag: E.meta, class: "tok-meta" },
  { tag: E.invalid, class: "tok-invalid" },
  { tag: E.punctuation, class: "tok-punctuation" }
]);
var As;
const zt = /* @__PURE__ */ new I();
function Qd(n) {
  return M.define({
    combine: n ? (e) => e.concat(n) : void 0
  });
}
const ep = /* @__PURE__ */ new I();
class Ie {
  /**
  Construct a language object. If you need to invoke this
  directly, first define a data facet with
  [`defineLanguageFacet`](https://codemirror.net/6/docs/ref/#language.defineLanguageFacet), and then
  configure your parser to [attach](https://codemirror.net/6/docs/ref/#language.languageDataProp) it
  to the language's outer syntax node.
  */
  constructor(e, t, i = [], s = "") {
    this.data = e, this.name = s, N.prototype.hasOwnProperty("tree") || Object.defineProperty(N.prototype, "tree", { get() {
      return Ze(this);
    } }), this.parser = t, this.extension = [
      si.of(this),
      N.languageData.of((r, o, l) => {
        let a = pl(r, o, l), h = a.type.prop(zt);
        if (!h)
          return [];
        let c = r.facet(h), f = a.type.prop(ep);
        if (f) {
          let u = a.resolve(o - a.from, l);
          for (let d of f)
            if (d.test(u, r)) {
              let p = r.facet(d.facet);
              return d.type == "replace" ? p : p.concat(c);
            }
        }
        return c;
      })
    ].concat(i);
  }
  /**
  Query whether this language is active at the given position.
  */
  isActiveAt(e, t, i = -1) {
    return pl(e, t, i).type.prop(zt) == this.data;
  }
  /**
  Find the document regions that were parsed using this language.
  The returned regions will _include_ any nested languages rooted
  in this language, when those exist.
  */
  findRegions(e) {
    let t = e.facet(si);
    if (t?.data == this.data)
      return [{ from: 0, to: e.doc.length }];
    if (!t || !t.allowsNesting)
      return [];
    let i = [], s = (r, o) => {
      if (r.prop(zt) == this.data) {
        i.push({ from: o, to: o + r.length });
        return;
      }
      let l = r.prop(I.mounted);
      if (l) {
        if (l.tree.prop(zt) == this.data) {
          if (l.overlay)
            for (let a of l.overlay)
              i.push({ from: a.from + o, to: a.to + o });
          else
            i.push({ from: o, to: o + r.length });
          return;
        } else if (l.overlay) {
          let a = i.length;
          if (s(l.tree, l.overlay[0].from + o), i.length > a)
            return;
        }
      }
      for (let a = 0; a < r.children.length; a++) {
        let h = r.children[a];
        h instanceof G && s(h, r.positions[a] + o);
      }
    };
    return s(Ze(e), 0), i;
  }
  /**
  Indicates whether this language allows nested languages. The
  default implementation returns true.
  */
  get allowsNesting() {
    return !0;
  }
}
Ie.setState = /* @__PURE__ */ W.define();
function pl(n, e, t) {
  let i = n.facet(si), s = Ze(n).topNode;
  if (!i || i.allowsNesting)
    for (let r = s; r; r = r.enter(e, t, X.ExcludeBuffers | X.EnterBracketed))
      r.type.isTop && (s = r);
  return s;
}
function Ze(n) {
  let e = n.field(Ie.state, !1);
  return e ? e.tree : G.empty;
}
class tp {
  /**
  Create an input object for the given document.
  */
  constructor(e) {
    this.doc = e, this.cursorPos = 0, this.string = "", this.cursor = e.iter();
  }
  get length() {
    return this.doc.length;
  }
  syncTo(e) {
    return this.string = this.cursor.next(e - this.cursorPos).value, this.cursorPos = e + this.string.length, this.cursorPos - this.string.length;
  }
  chunk(e) {
    return this.syncTo(e), this.string;
  }
  get lineChunks() {
    return !0;
  }
  read(e, t) {
    let i = this.cursorPos - this.string.length;
    return e < i || t >= this.cursorPos ? this.doc.sliceString(e, t) : this.string.slice(e - i, t - i);
  }
}
let ui = null;
class ii {
  constructor(e, t, i = [], s, r, o, l, a) {
    this.parser = e, this.state = t, this.fragments = i, this.tree = s, this.treeLen = r, this.viewport = o, this.skipped = l, this.scheduleOn = a, this.parse = null, this.tempSkipped = [];
  }
  /**
  @internal
  */
  static create(e, t, i) {
    return new ii(e, t, [], G.empty, 0, i, [], null);
  }
  startParse() {
    return this.parser.startParse(new tp(this.state.doc), this.fragments);
  }
  /**
  @internal
  */
  work(e, t) {
    return t != null && t >= this.state.doc.length && (t = void 0), this.tree != G.empty && this.isDone(t ?? this.state.doc.length) ? (this.takeTree(), !0) : this.withContext(() => {
      var i;
      if (typeof e == "number") {
        let s = Date.now() + e;
        e = () => Date.now() > s;
      }
      for (this.parse || (this.parse = this.startParse()), t != null && (this.parse.stoppedAt == null || this.parse.stoppedAt > t) && t < this.state.doc.length && this.parse.stopAt(t); ; ) {
        let s = this.parse.advance();
        if (s)
          if (this.fragments = this.withoutTempSkipped(Et.addTree(s, this.fragments, this.parse.stoppedAt != null)), this.treeLen = (i = this.parse.stoppedAt) !== null && i !== void 0 ? i : this.state.doc.length, this.tree = s, this.parse = null, this.treeLen < (t ?? this.state.doc.length))
            this.parse = this.startParse();
          else
            return !0;
        if (e())
          return !1;
      }
    });
  }
  /**
  @internal
  */
  takeTree() {
    let e, t;
    this.parse && (e = this.parse.parsedPos) >= this.treeLen && ((this.parse.stoppedAt == null || this.parse.stoppedAt > e) && this.parse.stopAt(e), this.withContext(() => {
      for (; !(t = this.parse.advance()); )
        ;
    }), this.treeLen = e, this.tree = t, this.fragments = this.withoutTempSkipped(Et.addTree(this.tree, this.fragments, !0)), this.parse = null);
  }
  withContext(e) {
    let t = ui;
    ui = this;
    try {
      return e();
    } finally {
      ui = t;
    }
  }
  withoutTempSkipped(e) {
    for (let t; t = this.tempSkipped.pop(); )
      e = gl(e, t.from, t.to);
    return e;
  }
  /**
  @internal
  */
  changes(e, t) {
    let { fragments: i, tree: s, treeLen: r, viewport: o, skipped: l } = this;
    if (this.takeTree(), !e.empty) {
      let a = [];
      if (e.iterChangedRanges((h, c, f, u) => a.push({ fromA: h, toA: c, fromB: f, toB: u })), i = Et.applyChanges(i, a), s = G.empty, r = 0, o = { from: e.mapPos(o.from, -1), to: e.mapPos(o.to, 1) }, this.skipped.length) {
        l = [];
        for (let h of this.skipped) {
          let c = e.mapPos(h.from, 1), f = e.mapPos(h.to, -1);
          c < f && l.push({ from: c, to: f });
        }
      }
    }
    return new ii(this.parser, t, i, s, r, o, l, this.scheduleOn);
  }
  /**
  @internal
  */
  updateViewport(e) {
    if (this.viewport.from == e.from && this.viewport.to == e.to)
      return !1;
    this.viewport = e;
    let t = this.skipped.length;
    for (let i = 0; i < this.skipped.length; i++) {
      let { from: s, to: r } = this.skipped[i];
      s < e.to && r > e.from && (this.fragments = gl(this.fragments, s, r), this.skipped.splice(i--, 1));
    }
    return this.skipped.length >= t ? !1 : (this.reset(), !0);
  }
  /**
  @internal
  */
  reset() {
    this.parse && (this.takeTree(), this.parse = null);
  }
  /**
  Notify the parse scheduler that the given region was skipped
  because it wasn't in view, and the parse should be restarted
  when it comes into view.
  */
  skipUntilInView(e, t) {
    this.skipped.push({ from: e, to: t });
  }
  /**
  Returns a parser intended to be used as placeholder when
  asynchronously loading a nested parser. It'll skip its input and
  mark it as not-really-parsed, so that the next update will parse
  it again.
  
  When `until` is given, a reparse will be scheduled when that
  promise resolves.
  */
  static getSkippingParser(e) {
    return new class extends gh {
      createParse(t, i, s) {
        let r = s[0].from, o = s[s.length - 1].to;
        return {
          parsedPos: r,
          advance() {
            let a = ui;
            if (a) {
              for (let h of s)
                a.tempSkipped.push(h);
              e && (a.scheduleOn = a.scheduleOn ? Promise.all([a.scheduleOn, e]) : e);
            }
            return this.parsedPos = o, new G(we.none, [], [], o - r);
          },
          stoppedAt: null,
          stopAt() {
          }
        };
      }
    }();
  }
  /**
  @internal
  */
  isDone(e) {
    e = Math.min(e, this.state.doc.length);
    let t = this.fragments;
    return this.treeLen >= e && t.length && t[0].from == 0 && t[0].to >= e;
  }
  /**
  Get the context for the current parse, or `null` if no editor
  parse is in progress.
  */
  static get() {
    return ui;
  }
}
function gl(n, e, t) {
  return Et.applyChanges(n, [{ fromA: e, toA: t, fromB: e, toB: t }]);
}
class ni {
  constructor(e) {
    this.context = e, this.tree = e.tree;
  }
  apply(e) {
    if (!e.docChanged && this.tree == this.context.tree)
      return this;
    let t = this.context.changes(e.changes, e.state), i = this.context.treeLen == e.startState.doc.length ? void 0 : Math.max(e.changes.mapPos(this.context.treeLen), t.viewport.to);
    return t.work(20, i) || t.takeTree(), new ni(t);
  }
  static init(e) {
    let t = Math.min(3e3, e.doc.length), i = ii.create(e.facet(si).parser, e, { from: 0, to: t });
    return i.work(20, t) || i.takeTree(), new ni(i);
  }
}
Ie.state = /* @__PURE__ */ Ae.define({
  create: ni.init,
  update(n, e) {
    for (let t of e.effects)
      if (t.is(Ie.setState))
        return t.value;
    return e.startState.facet(si) != e.state.facet(si) ? ni.init(e.state) : n.apply(e);
  }
});
let yh = (n) => {
  let e = setTimeout(
    () => n(),
    500
    /* Work.MaxPause */
  );
  return () => clearTimeout(e);
};
typeof requestIdleCallback < "u" && (yh = (n) => {
  let e = -1, t = setTimeout(
    () => {
      e = requestIdleCallback(n, {
        timeout: 400
        /* Work.MinPause */
      });
    },
    100
    /* Work.MinPause */
  );
  return () => e < 0 ? clearTimeout(t) : cancelIdleCallback(e);
});
const Cs = typeof navigator < "u" && (!((As = navigator.scheduling) === null || As === void 0) && As.isInputPending) ? () => navigator.scheduling.isInputPending() : null, ip = /* @__PURE__ */ be.fromClass(class {
  constructor(e) {
    this.view = e, this.working = null, this.workScheduled = 0, this.chunkEnd = -1, this.chunkBudget = -1, this.work = this.work.bind(this), this.scheduleWork();
  }
  update(e) {
    let t = this.view.state.field(Ie.state).context;
    (t.updateViewport(e.view.viewport) || this.view.viewport.to > t.treeLen) && this.scheduleWork(), (e.docChanged || e.selectionSet) && (this.view.hasFocus && (this.chunkBudget += 50), this.scheduleWork()), this.checkAsyncSchedule(t);
  }
  scheduleWork() {
    if (this.working)
      return;
    let { state: e } = this.view, t = e.field(Ie.state);
    (t.tree != t.context.tree || !t.context.isDone(e.doc.length)) && (this.working = yh(this.work));
  }
  work(e) {
    this.working = null;
    let t = Date.now();
    if (this.chunkEnd < t && (this.chunkEnd < 0 || this.view.hasFocus) && (this.chunkEnd = t + 3e4, this.chunkBudget = 3e3), this.chunkBudget <= 0)
      return;
    let { state: i, viewport: { to: s } } = this.view, r = i.field(Ie.state);
    if (r.tree == r.context.tree && r.context.isDone(
      s + 1e5
      /* Work.MaxParseAhead */
    ))
      return;
    let o = Date.now() + Math.min(this.chunkBudget, 100, e && !Cs ? Math.max(25, e.timeRemaining() - 5) : 1e9), l = r.context.treeLen < s && i.doc.length > s + 1e3, a = r.context.work(() => Cs && Cs() || Date.now() > o, s + (l ? 0 : 1e5));
    this.chunkBudget -= Date.now() - t, (a || this.chunkBudget <= 0) && (r.context.takeTree(), this.view.dispatch({ effects: Ie.setState.of(new ni(r.context)) })), this.chunkBudget > 0 && !(a && !l) && this.scheduleWork(), this.checkAsyncSchedule(r.context);
  }
  checkAsyncSchedule(e) {
    e.scheduleOn && (this.workScheduled++, e.scheduleOn.then(() => this.scheduleWork()).catch((t) => Pe(this.view.state, t)).then(() => this.workScheduled--), e.scheduleOn = null);
  }
  destroy() {
    this.working && this.working();
  }
  isWorking() {
    return !!(this.working || this.workScheduled > 0);
  }
}, {
  eventHandlers: { focus() {
    this.scheduleWork();
  } }
}), si = /* @__PURE__ */ M.define({
  combine(n) {
    return n.length ? n[0] : null;
  },
  enables: (n) => [
    Ie.state,
    ip,
    T.contentAttributes.compute([n], (e) => {
      let t = e.facet(n);
      return t && t.name ? { "data-language": t.name } : {};
    })
  ]
}), np = /* @__PURE__ */ M.define(), zr = /* @__PURE__ */ M.define({
  combine: (n) => {
    if (!n.length)
      return "  ";
    let e = n[0];
    if (!e || /\S/.test(e) || Array.from(e).some((t) => t != e[0]))
      throw new Error("Invalid indent unit: " + JSON.stringify(n[0]));
    return e;
  }
});
function $t(n) {
  let e = n.facet(zr);
  return e.charCodeAt(0) == 9 ? n.tabSize * e.length : e.length;
}
function Vn(n, e) {
  let t = "", i = n.tabSize, s = n.facet(zr)[0];
  if (s == "	") {
    for (; e >= i; )
      t += "	", e -= i;
    s = " ";
  }
  for (let r = 0; r < e; r++)
    t += s;
  return t;
}
function wh(n, e) {
  n instanceof N && (n = new Xn(n));
  for (let i of n.state.facet(np)) {
    let s = i(n, e);
    if (s !== void 0)
      return s;
  }
  let t = Ze(n.state);
  return t.length >= e ? sp(n, t, e) : null;
}
class Xn {
  /**
  Create an indent context.
  */
  constructor(e, t = {}) {
    this.state = e, this.options = t, this.unit = $t(e);
  }
  /**
  Get a description of the line at the given position, taking
  [simulated line
  breaks](https://codemirror.net/6/docs/ref/#language.IndentContext.constructor^options.simulateBreak)
  into account. If there is such a break at `pos`, the `bias`
  argument determines whether the part of the line line before or
  after the break is used.
  */
  lineAt(e, t = 1) {
    let i = this.state.doc.lineAt(e), { simulateBreak: s, simulateDoubleBreak: r } = this.options;
    return s != null && s >= i.from && s <= i.to ? r && s == e ? { text: "", from: e } : (t < 0 ? s < e : s <= e) ? { text: i.text.slice(s - i.from), from: s } : { text: i.text.slice(0, s - i.from), from: i.from } : i;
  }
  /**
  Get the text directly after `pos`, either the entire line
  or the next 100 characters, whichever is shorter.
  */
  textAfterPos(e, t = 1) {
    if (this.options.simulateDoubleBreak && e == this.options.simulateBreak)
      return "";
    let { text: i, from: s } = this.lineAt(e, t);
    return i.slice(e - s, Math.min(i.length, e + 100 - s));
  }
  /**
  Find the column for the given position.
  */
  column(e, t = 1) {
    let { text: i, from: s } = this.lineAt(e, t), r = this.countColumn(i, e - s), o = this.options.overrideIndentation ? this.options.overrideIndentation(s) : -1;
    return o > -1 && (r += o - this.countColumn(i, i.search(/\S|$/))), r;
  }
  /**
  Find the column position (taking tabs into account) of the given
  position in the given string.
  */
  countColumn(e, t = e.length) {
    return Un(e, this.state.tabSize, t);
  }
  /**
  Find the indentation column of the line at the given point.
  */
  lineIndent(e, t = 1) {
    let { text: i, from: s } = this.lineAt(e, t), r = this.options.overrideIndentation;
    if (r) {
      let o = r(s);
      if (o > -1)
        return o;
    }
    return this.countColumn(i, i.search(/\S|$/));
  }
  /**
  Returns the [simulated line
  break](https://codemirror.net/6/docs/ref/#language.IndentContext.constructor^options.simulateBreak)
  for this context, if any.
  */
  get simulatedBreak() {
    return this.options.simulateBreak || null;
  }
}
const vh = /* @__PURE__ */ new I();
function sp(n, e, t) {
  let i = e.resolveStack(t), s = e.resolveInner(t, -1).resolve(t, 0).enterUnfinishedNodesBefore(t);
  if (s != i.node) {
    let r = [];
    for (let o = s; o && !(o.from < i.node.from || o.to > i.node.to || o.from == i.node.from && o.type == i.node.type); o = o.parent)
      r.push(o);
    for (let o = r.length - 1; o >= 0; o--)
      i = { node: r[o], next: i };
  }
  return xh(i, n, t);
}
function xh(n, e, t) {
  for (let i = n; i; i = i.next) {
    let s = op(i.node);
    if (s)
      return s(Ur.create(e, t, i));
  }
  return 0;
}
function rp(n) {
  return n.pos == n.options.simulateBreak && n.options.simulateDoubleBreak;
}
function op(n) {
  let e = n.type.prop(vh);
  if (e)
    return e;
  let t = n.firstChild, i;
  if (t && (i = t.type.prop(I.closedBy))) {
    let s = n.lastChild, r = s && i.indexOf(s.name) > -1;
    return (o) => cp(o, !0, 1, void 0, r && !rp(o) ? s.from : void 0);
  }
  return n.parent == null ? lp : null;
}
function lp() {
  return 0;
}
class Ur extends Xn {
  constructor(e, t, i) {
    super(e.state, e.options), this.base = e, this.pos = t, this.context = i;
  }
  /**
  The syntax tree node to which the indentation strategy
  applies.
  */
  get node() {
    return this.context.node;
  }
  /**
  @internal
  */
  static create(e, t, i) {
    return new Ur(e, t, i);
  }
  /**
  Get the text directly after `this.pos`, either the entire line
  or the next 100 characters, whichever is shorter.
  */
  get textAfter() {
    return this.textAfterPos(this.pos);
  }
  /**
  Get the indentation at the reference line for `this.node`, which
  is the line on which it starts, unless there is a node that is
  _not_ a parent of this node covering the start of that line. If
  so, the line at the start of that node is tried, again skipping
  on if it is covered by another such node.
  */
  get baseIndent() {
    return this.baseIndentFor(this.node);
  }
  /**
  Get the indentation for the reference line of the given node
  (see [`baseIndent`](https://codemirror.net/6/docs/ref/#language.TreeIndentContext.baseIndent)).
  */
  baseIndentFor(e) {
    let t = this.state.doc.lineAt(e.from);
    for (; ; ) {
      let i = e.resolve(t.from);
      for (; i.parent && i.parent.from == i.from; )
        i = i.parent;
      if (ap(i, e))
        break;
      t = this.state.doc.lineAt(i.from);
    }
    return this.lineIndent(t.from);
  }
  /**
  Continue looking for indentations in the node's parent nodes,
  and return the result of that.
  */
  continue() {
    return xh(this.context.next, this.base, this.pos);
  }
}
function ap(n, e) {
  for (let t = e; t; t = t.parent)
    if (n == t)
      return !0;
  return !1;
}
function hp(n) {
  let e = n.node, t = e.childAfter(e.from), i = e.lastChild;
  if (!t)
    return null;
  let s = n.options.simulateBreak, r = n.state.doc.lineAt(t.from), o = s == null || s <= r.from ? r.to : Math.min(r.to, s);
  for (let l = t.to; ; ) {
    let a = e.childAfter(l);
    if (!a || a == i)
      return null;
    if (!a.type.isSkipped) {
      if (a.from >= o)
        return null;
      let h = /^ */.exec(r.text.slice(t.to - r.from))[0].length;
      return { from: t.from, to: t.to + h };
    }
    l = a.to;
  }
}
function cp(n, e, t, i, s) {
  let r = n.textAfter, o = r.match(/^\s*/)[0].length, l = i && r.slice(o, o + i.length) == i || s == n.pos + o, a = hp(n);
  return a ? l ? n.column(a.from) : n.column(a.to) : n.baseIndent + (l ? 0 : n.unit * t);
}
class Zn {
  constructor(e, t) {
    this.specs = e;
    let i;
    function s(l) {
      let a = dt.newName();
      return (i || (i = /* @__PURE__ */ Object.create(null)))["." + a] = l, a;
    }
    const r = typeof t.all == "string" ? t.all : t.all ? s(t.all) : void 0, o = t.scope;
    this.scope = o instanceof Ie ? (l) => l.prop(zt) == o.data : o ? (l) => l == o : void 0, this.style = bh(e.map((l) => ({
      tag: l.tag,
      class: l.class || s(Object.assign({}, l, { tag: null }))
    })), {
      all: r
    }).style, this.module = i ? new dt(i) : null, this.themeType = t.themeType;
  }
  /**
  Create a highlighter style that associates the given styles to
  the given tags. The specs must be objects that hold a style tag
  or array of tags in their `tag` property, and either a single
  `class` property providing a static CSS class (for highlighter
  that rely on external styling), or a
  [`style-mod`](https://github.com/marijnh/style-mod#documentation)-style
  set of CSS properties (which define the styling for those tags).
  
  The CSS rules created for a highlighter will be emitted in the
  order of the spec's properties. That means that for elements that
  have multiple tags associated with them, styles defined further
  down in the list will have a higher CSS precedence than styles
  defined earlier.
  */
  static define(e, t) {
    return new Zn(e, t || {});
  }
}
const pr = /* @__PURE__ */ M.define(), fp = /* @__PURE__ */ M.define({
  combine(n) {
    return n.length ? [n[0]] : null;
  }
});
function Ms(n) {
  let e = n.facet(pr);
  return e.length ? e : n.facet(fp);
}
function up(n, e) {
  let t = [pp], i;
  return n instanceof Zn && (n.module && t.push(T.styleModule.of(n.module)), i = n.themeType), i ? t.push(pr.computeN([T.darkTheme], (s) => s.facet(T.darkTheme) == (i == "dark") ? [n] : [])) : t.push(pr.of(n)), t;
}
class dp {
  constructor(e) {
    this.markCache = /* @__PURE__ */ Object.create(null), this.tree = Ze(e.state), this.decorations = this.buildDeco(e, Ms(e.state)), this.decoratedTo = e.viewport.to;
  }
  update(e) {
    let t = Ze(e.state), i = Ms(e.state), s = i != Ms(e.startState), { viewport: r } = e.view, o = e.changes.mapPos(this.decoratedTo, 1);
    t.length < r.to && !s && t.type == this.tree.type && o >= r.to ? (this.decorations = this.decorations.map(e.changes), this.decoratedTo = o) : (t != this.tree || e.viewportChanged || s) && (this.tree = t, this.decorations = this.buildDeco(e.view, i), this.decoratedTo = r.to);
  }
  buildDeco(e, t) {
    if (!t || !this.tree.length)
      return K.none;
    let i = new Xt();
    for (let { from: s, to: r } of e.visibleRanges)
      Yd(this.tree, t, (o, l, a) => {
        i.add(o, l, this.markCache[a] || (this.markCache[a] = K.mark({ class: a })));
      }, s, r);
    return i.finish();
  }
}
const pp = /* @__PURE__ */ zn.high(/* @__PURE__ */ be.fromClass(dp, {
  decorations: (n) => n.decorations
})), gp = 1e4, mp = "()[]{}", bp = /* @__PURE__ */ new I();
function gr(n, e, t) {
  let i = n.prop(e < 0 ? I.openedBy : I.closedBy);
  if (i)
    return i;
  if (n.name.length == 1) {
    let s = t.indexOf(n.name);
    if (s > -1 && s % 2 == (e < 0 ? 1 : 0))
      return [t[s + e]];
  }
  return null;
}
function mr(n) {
  let e = n.type.prop(bp);
  return e ? e(n.node) : n;
}
function Ut(n, e, t, i = {}) {
  let s = i.maxScanDistance || gp, r = i.brackets || mp, o = Ze(n), l = o.resolveInner(e, t);
  for (let a = l; a; a = a.parent) {
    let h = gr(a.type, t, r);
    if (h && a.from < a.to) {
      let c = mr(a);
      if (c && (t > 0 ? e >= c.from && e < c.to : e > c.from && e <= c.to))
        return yp(n, e, t, a, c, h, r);
    }
  }
  return wp(n, e, t, o, l.type, s, r);
}
function yp(n, e, t, i, s, r, o) {
  let l = i.parent, a = { from: s.from, to: s.to }, h = 0, c = l?.cursor();
  if (c && (t < 0 ? c.childBefore(i.from) : c.childAfter(i.to)))
    do
      if (t < 0 ? c.to <= i.from : c.from >= i.to) {
        if (h == 0 && r.indexOf(c.type.name) > -1 && c.from < c.to) {
          let f = mr(c);
          return { start: a, end: f ? { from: f.from, to: f.to } : void 0, matched: !0 };
        } else if (gr(c.type, t, o))
          h++;
        else if (gr(c.type, -t, o)) {
          if (h == 0) {
            let f = mr(c);
            return {
              start: a,
              end: f && f.from < f.to ? { from: f.from, to: f.to } : void 0,
              matched: !1
            };
          }
          h--;
        }
      }
    while (t < 0 ? c.prevSibling() : c.nextSibling());
  return { start: a, matched: !1 };
}
function wp(n, e, t, i, s, r, o) {
  if (t < 0 ? !e : e == n.doc.length)
    return null;
  let l = t < 0 ? n.sliceDoc(e - 1, e) : n.sliceDoc(e, e + 1), a = o.indexOf(l);
  if (a < 0 || a % 2 == 0 != t > 0)
    return null;
  let h = { from: t < 0 ? e - 1 : e, to: t > 0 ? e + 1 : e }, c = n.doc.iterRange(e, t > 0 ? n.doc.length : 0), f = 0;
  for (let u = 0; !c.next().done && u <= r; ) {
    let d = c.value;
    t < 0 && (u += d.length);
    let p = e + u * t;
    for (let g = t > 0 ? 0 : d.length - 1, m = t > 0 ? d.length : -1; g != m; g += t) {
      let b = o.indexOf(d[g]);
      if (!(b < 0 || i.resolveInner(p + g, 1).type != s))
        if (b % 2 == 0 == t > 0)
          f++;
        else {
          if (f == 1)
            return { start: h, end: { from: p + g, to: p + g + 1 }, matched: b >> 1 == a >> 1 };
          f--;
        }
    }
    t > 0 && (u += d.length);
  }
  return c.done ? { start: h, matched: !1 } : null;
}
function ml(n, e, t, i = 0, s = 0) {
  e == null && (e = n.search(/[^\s\u00a0]/), e == -1 && (e = n.length));
  let r = s;
  for (let o = i; o < e; o++)
    n.charCodeAt(o) == 9 ? r += t - r % t : r++;
  return r;
}
class kh {
  /**
  Create a stream.
  */
  constructor(e, t, i, s) {
    this.string = e, this.tabSize = t, this.indentUnit = i, this.overrideIndent = s, this.pos = 0, this.start = 0, this.lastColumnPos = 0, this.lastColumnValue = 0;
  }
  /**
  True if we are at the end of the line.
  */
  eol() {
    return this.pos >= this.string.length;
  }
  /**
  True if we are at the start of the line.
  */
  sol() {
    return this.pos == 0;
  }
  /**
  Get the next code unit after the current position, or undefined
  if we're at the end of the line.
  */
  peek() {
    return this.string.charAt(this.pos) || void 0;
  }
  /**
  Read the next code unit and advance `this.pos`.
  */
  next() {
    if (this.pos < this.string.length)
      return this.string.charAt(this.pos++);
  }
  /**
  Match the next character against the given string, regular
  expression, or predicate. Consume and return it if it matches.
  */
  eat(e) {
    let t = this.string.charAt(this.pos), i;
    if (typeof e == "string" ? i = t == e : i = t && (e instanceof RegExp ? e.test(t) : e(t)), i)
      return ++this.pos, t;
  }
  /**
  Continue matching characters that match the given string,
  regular expression, or predicate function. Return true if any
  characters were consumed.
  */
  eatWhile(e) {
    let t = this.pos;
    for (; this.eat(e); )
      ;
    return this.pos > t;
  }
  /**
  Consume whitespace ahead of `this.pos`. Return true if any was
  found.
  */
  eatSpace() {
    let e = this.pos;
    for (; /[\s\u00a0]/.test(this.string.charAt(this.pos)); )
      ++this.pos;
    return this.pos > e;
  }
  /**
  Move to the end of the line.
  */
  skipToEnd() {
    this.pos = this.string.length;
  }
  /**
  Move to directly before the given character, if found on the
  current line.
  */
  skipTo(e) {
    let t = this.string.indexOf(e, this.pos);
    if (t > -1)
      return this.pos = t, !0;
  }
  /**
  Move back `n` characters.
  */
  backUp(e) {
    this.pos -= e;
  }
  /**
  Get the column position at `this.pos`.
  */
  column() {
    return this.lastColumnPos < this.start && (this.lastColumnValue = ml(this.string, this.start, this.tabSize, this.lastColumnPos, this.lastColumnValue), this.lastColumnPos = this.start), this.lastColumnValue;
  }
  /**
  Get the indentation column of the current line.
  */
  indentation() {
    var e;
    return (e = this.overrideIndent) !== null && e !== void 0 ? e : ml(this.string, null, this.tabSize);
  }
  /**
  Match the input against the given string or regular expression
  (which should start with a `^`). Return true or the regexp match
  if it matches.
  
  Unless `consume` is set to `false`, this will move `this.pos`
  past the matched text.
  
  When matching a string `caseInsensitive` can be set to true to
  make the match case-insensitive.
  */
  match(e, t, i) {
    if (typeof e == "string") {
      let s = (o) => i ? o.toLowerCase() : o, r = this.string.substr(this.pos, e.length);
      return s(r) == s(e) ? (t !== !1 && (this.pos += e.length), !0) : null;
    } else {
      let s = this.string.slice(this.pos).match(e);
      return s && s.index > 0 ? null : (s && t !== !1 && (this.pos += s[0].length), s);
    }
  }
  /**
  Get the current token.
  */
  current() {
    return this.string.slice(this.start, this.pos);
  }
}
function vp(n) {
  return {
    name: n.name || "",
    token: n.token,
    blankLine: n.blankLine || (() => {
    }),
    startState: n.startState || (() => !0),
    copyState: n.copyState || xp,
    indent: n.indent || (() => null),
    languageData: n.languageData || {},
    tokenTable: n.tokenTable || Kr,
    mergeTokens: n.mergeTokens !== !1
  };
}
function xp(n) {
  if (typeof n != "object")
    return n;
  let e = {};
  for (let t in n) {
    let i = n[t];
    e[t] = i instanceof Array ? i.slice() : i;
  }
  return e;
}
const bl = /* @__PURE__ */ new WeakMap();
class jr extends Ie {
  constructor(e) {
    let t = Qd(e.languageData), i = vp(e), s, r = new class extends gh {
      createParse(o, l, a) {
        return new Sp(s, o, l, a);
      }
    }();
    super(t, r, [], e.name), this.topNode = Mp(t, this), s = this, this.streamParser = i, this.stateAfter = new I({ perNode: !0 }), this.tokenTable = e.tokenTable ? new Mh(i.tokenTable) : Cp;
  }
  /**
  Define a stream language.
  */
  static define(e) {
    return new jr(e);
  }
  /**
  @internal
  */
  getIndent(e) {
    let t, { overrideIndentation: i } = e.options;
    i && (t = bl.get(e.state), t != null && t < e.pos - 1e4 && (t = void 0));
    let s = qr(this, e.node.tree, e.node.from, e.node.from, t ?? e.pos), r, o;
    if (s ? (o = s.state, r = s.pos + 1) : (o = this.streamParser.startState(e.unit), r = e.node.from), e.pos - r > 1e4)
      return null;
    for (; r < e.pos; ) {
      let a = e.state.doc.lineAt(r), h = Math.min(e.pos, a.to);
      if (a.length) {
        let c = i ? i(a.from) : -1, f = new kh(a.text, e.state.tabSize, e.unit, c < 0 ? void 0 : c);
        for (; f.pos < h - a.from; )
          Ah(this.streamParser.token, f, o);
      } else
        this.streamParser.blankLine(o, e.unit);
      if (h == e.pos)
        break;
      r = a.to + 1;
    }
    let l = e.lineAt(e.pos);
    return i && t == null && bl.set(e.state, l.from), this.streamParser.indent(o, /^\s*(.*)/.exec(l.text)[1], e);
  }
  get allowsNesting() {
    return !1;
  }
}
function qr(n, e, t, i, s) {
  let r = t >= i && t + e.length <= s && e.prop(n.stateAfter);
  if (r)
    return { state: n.streamParser.copyState(r), pos: t + e.length };
  for (let o = e.children.length - 1; o >= 0; o--) {
    let l = e.children[o], a = t + e.positions[o], h = l instanceof G && a < s && qr(n, l, a, i, s);
    if (h)
      return h;
  }
  return null;
}
function Sh(n, e, t, i, s) {
  if (s && t <= 0 && i >= e.length)
    return e;
  !s && t == 0 && e.type == n.topNode && (s = !0);
  for (let r = e.children.length - 1; r >= 0; r--) {
    let o = e.positions[r], l = e.children[r], a;
    if (o < i && l instanceof G) {
      if (!(a = Sh(n, l, t - o, i - o, s)))
        break;
      return s ? new G(e.type, e.children.slice(0, r).concat(a), e.positions.slice(0, r + 1), o + a.length) : a;
    }
  }
  return null;
}
function kp(n, e, t, i, s) {
  for (let r of e) {
    let o = r.from + (r.openStart ? 25 : 0), l = r.to - (r.openEnd ? 25 : 0), a = o <= t && l > t && qr(n, r.tree, 0 - r.offset, t, l), h;
    if (a && a.pos <= i && (h = Sh(n, r.tree, t + r.offset, a.pos + r.offset, !1)))
      return { state: a.state, tree: h };
  }
  return { state: n.streamParser.startState(s ? $t(s) : 4), tree: G.empty };
}
class Sp {
  constructor(e, t, i, s) {
    this.lang = e, this.input = t, this.fragments = i, this.ranges = s, this.stoppedAt = null, this.chunks = [], this.chunkPos = [], this.chunk = [], this.chunkReused = void 0, this.rangeIndex = 0, this.to = s[s.length - 1].to;
    let r = ii.get(), o = s[0].from, { state: l, tree: a } = kp(e, i, o, this.to, r?.state);
    this.state = l, this.parsedPos = this.chunkStart = o + a.length;
    for (let h = 0; h < a.children.length; h++)
      this.chunks.push(a.children[h]), this.chunkPos.push(a.positions[h]);
    r && this.parsedPos < r.viewport.from - 1e5 && s.some((h) => h.from <= r.viewport.from && h.to >= r.viewport.from) && (this.state = this.lang.streamParser.startState($t(r.state)), r.skipUntilInView(this.parsedPos, r.viewport.from), this.parsedPos = r.viewport.from), this.moveRangeIndex();
  }
  advance() {
    let e = ii.get(), t = this.stoppedAt == null ? this.to : Math.min(this.to, this.stoppedAt), i = Math.min(
      t,
      this.chunkStart + 512
      /* C.ChunkSize */
    );
    for (e && (i = Math.min(i, e.viewport.to)); this.parsedPos < i; )
      this.parseLine(e);
    return this.chunkStart < this.parsedPos && this.finishChunk(), this.parsedPos >= t ? this.finish() : e && this.parsedPos >= e.viewport.to ? (e.skipUntilInView(this.parsedPos, t), this.finish()) : null;
  }
  stopAt(e) {
    this.stoppedAt = e;
  }
  lineAfter(e) {
    let t = this.input.chunk(e);
    if (this.input.lineChunks)
      t == `
` && (t = "");
    else {
      let i = t.indexOf(`
`);
      i > -1 && (t = t.slice(0, i));
    }
    return e + t.length <= this.to ? t : t.slice(0, this.to - e);
  }
  nextLine() {
    let e = this.parsedPos, t = this.lineAfter(e), i = e + t.length;
    for (let s = this.rangeIndex; ; ) {
      let r = this.ranges[s].to;
      if (r >= i || (t = t.slice(0, r - (i - t.length)), s++, s == this.ranges.length))
        break;
      let o = this.ranges[s].from, l = this.lineAfter(o);
      t += l, i = o + l.length;
    }
    return { line: t, end: i };
  }
  skipGapsTo(e, t, i) {
    for (; ; ) {
      let s = this.ranges[this.rangeIndex].to, r = e + t;
      if (i > 0 ? s > r : s >= r)
        break;
      let o = this.ranges[++this.rangeIndex].from;
      t += o - s;
    }
    return t;
  }
  moveRangeIndex() {
    for (; this.ranges[this.rangeIndex].to < this.parsedPos; )
      this.rangeIndex++;
  }
  emitToken(e, t, i, s) {
    let r = 4;
    if (this.ranges.length > 1) {
      s = this.skipGapsTo(t, s, 1), t += s;
      let l = this.chunk.length;
      s = this.skipGapsTo(i, s, -1), i += s, r += this.chunk.length - l;
    }
    let o = this.chunk.length - 4;
    return this.lang.streamParser.mergeTokens && r == 4 && o >= 0 && this.chunk[o] == e && this.chunk[o + 2] == t ? this.chunk[o + 2] = i : this.chunk.push(e, t, i, r), s;
  }
  parseLine(e) {
    let { line: t, end: i } = this.nextLine(), s = 0, { streamParser: r } = this.lang, o = new kh(t, e ? e.state.tabSize : 4, e ? $t(e.state) : 2);
    if (o.eol())
      r.blankLine(this.state, o.indentUnit);
    else
      for (; !o.eol(); ) {
        let l = Ah(r.token, o, this.state);
        if (l && (s = this.emitToken(this.lang.tokenTable.resolve(l), this.parsedPos + o.start, this.parsedPos + o.pos, s)), o.start > 1e4)
          break;
      }
    this.parsedPos = i, this.moveRangeIndex(), this.parsedPos < this.to && this.parsedPos++;
  }
  finishChunk() {
    let e = G.build({
      buffer: this.chunk,
      start: this.chunkStart,
      length: this.parsedPos - this.chunkStart,
      nodeSet: Ap,
      topID: 0,
      maxBufferLength: 512,
      reused: this.chunkReused
    });
    e = new G(e.type, e.children, e.positions, e.length, [[this.lang.stateAfter, this.lang.streamParser.copyState(this.state)]]), this.chunks.push(e), this.chunkPos.push(this.chunkStart - this.ranges[0].from), this.chunk = [], this.chunkReused = void 0, this.chunkStart = this.parsedPos;
  }
  finish() {
    return new G(this.lang.topNode, this.chunks, this.chunkPos, this.parsedPos - this.ranges[0].from).balance();
  }
}
function Ah(n, e, t) {
  e.start = e.pos;
  for (let i = 0; i < 10; i++) {
    let s = n(e, t);
    if (e.pos > e.start)
      return s;
  }
  throw new Error("Stream parser failed to advance stream.");
}
const Kr = /* @__PURE__ */ Object.create(null), $i = [we.none], Ap = /* @__PURE__ */ new Vr($i), yl = [], wl = /* @__PURE__ */ Object.create(null), Ch = /* @__PURE__ */ Object.create(null);
for (let [n, e] of [
  ["variable", "variableName"],
  ["variable-2", "variableName.special"],
  ["string-2", "string.special"],
  ["def", "variableName.definition"],
  ["tag", "tagName"],
  ["attribute", "attributeName"],
  ["type", "typeName"],
  ["builtin", "variableName.standard"],
  ["qualifier", "modifier"],
  ["error", "invalid"],
  ["header", "heading"],
  ["property", "propertyName"]
])
  Ch[n] = /* @__PURE__ */ Th(Kr, e);
class Mh {
  constructor(e) {
    this.extra = e, this.table = Object.assign(/* @__PURE__ */ Object.create(null), Ch);
  }
  resolve(e) {
    return e ? this.table[e] || (this.table[e] = Th(this.extra, e)) : 0;
  }
}
const Cp = /* @__PURE__ */ new Mh(Kr);
function Ts(n, e) {
  yl.indexOf(n) > -1 || (yl.push(n), console.warn(e));
}
function Th(n, e) {
  let t = [];
  for (let l of e.split(" ")) {
    let a = [];
    for (let h of l.split(".")) {
      let c = n[h] || E[h];
      c ? typeof c == "function" ? a.length ? a = a.map(c) : Ts(h, `Modifier ${h} used at start of tag`) : a.length ? Ts(h, `Tag ${h} used as modifier`) : a = Array.isArray(c) ? c : [c] : Ts(h, `Unknown highlighting tag ${h}`);
    }
    for (let h of a)
      t.push(h);
  }
  if (!t.length)
    return 0;
  let i = e.replace(/ /g, "_"), s = i + " " + t.map((l) => l.id), r = wl[s];
  if (r)
    return r.id;
  let o = wl[s] = we.define({
    id: $i.length,
    name: i,
    props: [Gd({ [i]: t })]
  });
  return $i.push(o), o.id;
}
function Mp(n, e) {
  let t = we.define({ id: $i.length, name: "Document", props: [
    zt.add(() => n),
    vh.add(() => (i) => e.getIndent(i))
  ], top: !0 });
  return $i.push(t), t;
}
q.RTL, q.LTR;
const Tp = (n) => {
  let { state: e } = n, t = e.doc.lineAt(e.selection.main.from), i = Jr(n.state, t.from);
  return i.line ? Op(n) : i.block ? Ep(n) : !1;
};
function Gr(n, e) {
  return ({ state: t, dispatch: i }) => {
    if (t.readOnly)
      return !1;
    let s = n(e, t);
    return s ? (i(t.update(s)), !0) : !1;
  };
}
const Op = /* @__PURE__ */ Gr(
  Bp,
  0
  /* CommentOption.Toggle */
), Dp = /* @__PURE__ */ Gr(
  Oh,
  0
  /* CommentOption.Toggle */
), Ep = /* @__PURE__ */ Gr(
  (n, e) => Oh(n, e, Rp(e)),
  0
  /* CommentOption.Toggle */
);
function Jr(n, e) {
  let t = n.languageDataAt("commentTokens", e, 1);
  return t.length ? t[0] : {};
}
const di = 50;
function Lp(n, { open: e, close: t }, i, s) {
  let r = n.sliceDoc(i - di, i), o = n.sliceDoc(s, s + di), l = /\s*$/.exec(r)[0].length, a = /^\s*/.exec(o)[0].length, h = r.length - l;
  if (r.slice(h - e.length, h) == e && o.slice(a, a + t.length) == t)
    return {
      open: { pos: i - l, margin: l && 1 },
      close: { pos: s + a, margin: a && 1 }
    };
  let c, f;
  s - i <= 2 * di ? c = f = n.sliceDoc(i, s) : (c = n.sliceDoc(i, i + di), f = n.sliceDoc(s - di, s));
  let u = /^\s*/.exec(c)[0].length, d = /\s*$/.exec(f)[0].length, p = f.length - d - t.length;
  return c.slice(u, u + e.length) == e && f.slice(p, p + t.length) == t ? {
    open: {
      pos: i + u + e.length,
      margin: /\s/.test(c.charAt(u + e.length)) ? 1 : 0
    },
    close: {
      pos: s - d - t.length,
      margin: /\s/.test(f.charAt(p - 1)) ? 1 : 0
    }
  } : null;
}
function Rp(n) {
  let e = [];
  for (let t of n.selection.ranges) {
    let i = n.doc.lineAt(t.from), s = t.to <= i.to ? i : n.doc.lineAt(t.to);
    s.from > i.from && s.from == t.to && (s = t.to == i.to + 1 ? i : n.doc.lineAt(t.to - 1));
    let r = e.length - 1;
    r >= 0 && e[r].to > i.from ? e[r].to = s.to : e.push({ from: i.from + /^\s*/.exec(i.text)[0].length, to: s.to });
  }
  return e;
}
function Oh(n, e, t = e.selection.ranges) {
  let i = t.map((r) => Jr(e, r.from).block);
  if (!i.every((r) => r))
    return null;
  let s = t.map((r, o) => Lp(e, i[o], r.from, r.to));
  if (n != 2 && !s.every((r) => r))
    return { changes: e.changes(t.map((r, o) => s[o] ? [] : [{ from: r.from, insert: i[o].open + " " }, { from: r.to, insert: " " + i[o].close }])) };
  if (n != 1 && s.some((r) => r)) {
    let r = [];
    for (let o = 0, l; o < s.length; o++)
      if (l = s[o]) {
        let a = i[o], { open: h, close: c } = l;
        r.push({ from: h.pos - a.open.length, to: h.pos + h.margin }, { from: c.pos - c.margin, to: c.pos + a.close.length });
      }
    return { changes: r };
  }
  return null;
}
function Bp(n, e, t = e.selection.ranges) {
  let i = [], s = -1;
  e: for (let { from: r, to: o } of t) {
    let l = i.length, a = 1e9, h;
    for (let c = r; c <= o; ) {
      let f = e.doc.lineAt(c);
      if (h == null && (h = Jr(e, f.from).line, !h))
        continue e;
      if (f.from > s && (r == o || o > f.from)) {
        s = f.from;
        let u = /^\s*/.exec(f.text)[0].length, d = u == f.length, p = f.text.slice(u, u + h.length) == h ? u : -1;
        u < f.text.length && u < a && (a = u), i.push({ line: f, comment: p, token: h, indent: u, empty: d, single: !1 });
      }
      c = f.to + 1;
    }
    if (a < 1e9)
      for (let c = l; c < i.length; c++)
        i[c].indent < i[c].line.text.length && (i[c].indent = a);
    i.length == l + 1 && (i[l].single = !0);
  }
  if (n != 2 && i.some((r) => r.comment < 0 && (!r.empty || r.single))) {
    let r = [];
    for (let { line: l, token: a, indent: h, empty: c, single: f } of i)
      (f || !c) && r.push({ from: l.from + h, insert: a + " " });
    let o = e.changes(r);
    return { changes: o, selection: e.selection.map(o, 1) };
  } else if (n != 1 && i.some((r) => r.comment >= 0)) {
    let r = [];
    for (let { line: o, comment: l, token: a } of i)
      if (l >= 0) {
        let h = o.from + l, c = h + a.length;
        o.text[c - o.from] == " " && c++, r.push({ from: h, to: c });
      }
    return { changes: r };
  }
  return null;
}
const br = /* @__PURE__ */ yt.define(), Pp = /* @__PURE__ */ yt.define(), Ip = /* @__PURE__ */ M.define(), Dh = /* @__PURE__ */ M.define({
  combine(n) {
    return _i(n, {
      minDepth: 100,
      newGroupDelay: 500,
      joinToEvent: (e, t) => t
    }, {
      minDepth: Math.max,
      newGroupDelay: Math.min,
      joinToEvent: (e, t) => (i, s) => e(i, s) || t(i, s)
    });
  }
}), Eh = /* @__PURE__ */ Ae.define({
  create() {
    return Ye.empty;
  },
  update(n, e) {
    let t = e.state.facet(Dh), i = e.annotation(br);
    if (i) {
      let a = me.fromTransaction(e, i.selection), h = i.side, c = h == 0 ? n.undone : n.done;
      return a ? c = Fn(c, c.length, t.minDepth, a) : c = Bh(c, e.startState.selection), new Ye(h == 0 ? i.rest : c, h == 0 ? c : i.rest);
    }
    let s = e.annotation(Pp);
    if ((s == "full" || s == "before") && (n = n.isolate()), e.annotation(te.addToHistory) === !1)
      return e.changes.empty ? n : n.addMapping(e.changes.desc);
    let r = me.fromTransaction(e), o = e.annotation(te.time), l = e.annotation(te.userEvent);
    return r ? n = n.addChanges(r, o, l, t, e) : e.selection && (n = n.addSelection(e.startState.selection, o, l, t.newGroupDelay)), (s == "full" || s == "after") && (n = n.isolate()), n;
  },
  toJSON(n) {
    return { done: n.done.map((e) => e.toJSON()), undone: n.undone.map((e) => e.toJSON()) };
  },
  fromJSON(n) {
    return new Ye(n.done.map(me.fromJSON), n.undone.map(me.fromJSON));
  }
});
function $p(n = {}) {
  return [
    Eh,
    Dh.of(n),
    T.domEventHandlers({
      beforeinput(e, t) {
        let i = e.inputType == "historyUndo" ? Lh : e.inputType == "historyRedo" ? yr : null;
        return i ? (e.preventDefault(), i(t)) : !1;
      }
    })
  ];
}
function Qn(n, e) {
  return function({ state: t, dispatch: i }) {
    if (!e && t.readOnly)
      return !1;
    let s = t.field(Eh, !1);
    if (!s)
      return !1;
    let r = s.pop(n, t, e);
    return r ? (i(r), !0) : !1;
  };
}
const Lh = /* @__PURE__ */ Qn(0, !1), yr = /* @__PURE__ */ Qn(1, !1), Np = /* @__PURE__ */ Qn(0, !0), Hp = /* @__PURE__ */ Qn(1, !0);
class me {
  constructor(e, t, i, s, r) {
    this.changes = e, this.effects = t, this.mapped = i, this.startSelection = s, this.selectionsAfter = r;
  }
  setSelAfter(e) {
    return new me(this.changes, this.effects, this.mapped, this.startSelection, e);
  }
  toJSON() {
    var e, t, i;
    return {
      changes: (e = this.changes) === null || e === void 0 ? void 0 : e.toJSON(),
      mapped: (t = this.mapped) === null || t === void 0 ? void 0 : t.toJSON(),
      startSelection: (i = this.startSelection) === null || i === void 0 ? void 0 : i.toJSON(),
      selectionsAfter: this.selectionsAfter.map((s) => s.toJSON())
    };
  }
  static fromJSON(e) {
    return new me(e.changes && ee.fromJSON(e.changes), [], e.mapped && Xe.fromJSON(e.mapped), e.startSelection && y.fromJSON(e.startSelection), e.selectionsAfter.map(y.fromJSON));
  }
  // This does not check `addToHistory` and such, it assumes the
  // transaction needs to be converted to an item. Returns null when
  // there are no changes or effects in the transaction.
  static fromTransaction(e, t) {
    let i = De;
    for (let s of e.startState.facet(Ip)) {
      let r = s(e);
      r.length && (i = i.concat(r));
    }
    return !i.length && e.changes.empty ? null : new me(e.changes.invert(e.startState.doc), i, void 0, t || e.startState.selection, De);
  }
  static selection(e) {
    return new me(void 0, De, void 0, void 0, e);
  }
}
function Fn(n, e, t, i) {
  let s = e + 1 > t + 20 ? e - t - 1 : 0, r = n.slice(s, e);
  return r.push(i), r;
}
function Vp(n, e) {
  let t = [], i = !1;
  return n.iterChangedRanges((s, r) => t.push(s, r)), e.iterChangedRanges((s, r, o, l) => {
    for (let a = 0; a < t.length; ) {
      let h = t[a++], c = t[a++];
      l >= h && o <= c && (i = !0);
    }
  }), i;
}
function Fp(n, e) {
  return n.ranges.length == e.ranges.length && n.ranges.filter((t, i) => t.empty != e.ranges[i].empty).length === 0;
}
function Rh(n, e) {
  return n.length ? e.length ? n.concat(e) : n : e;
}
const De = [], Wp = 200;
function Bh(n, e) {
  if (n.length) {
    let t = n[n.length - 1], i = t.selectionsAfter.slice(Math.max(0, t.selectionsAfter.length - Wp));
    return i.length && i[i.length - 1].eq(e) ? n : (i.push(e), Fn(n, n.length - 1, 1e9, t.setSelAfter(i)));
  } else
    return [me.selection([e])];
}
function _p(n) {
  let e = n[n.length - 1], t = n.slice();
  return t[n.length - 1] = e.setSelAfter(e.selectionsAfter.slice(0, e.selectionsAfter.length - 1)), t;
}
function Os(n, e) {
  if (!n.length)
    return n;
  let t = n.length, i = De;
  for (; t; ) {
    let s = zp(n[t - 1], e, i);
    if (s.changes && !s.changes.empty || s.effects.length) {
      let r = n.slice(0, t);
      return r[t - 1] = s, r;
    } else
      e = s.mapped, t--, i = s.selectionsAfter;
  }
  return i.length ? [me.selection(i)] : De;
}
function zp(n, e, t) {
  let i = Rh(n.selectionsAfter.length ? n.selectionsAfter.map((l) => l.map(e)) : De, t);
  if (!n.changes)
    return me.selection(i);
  let s = n.changes.map(e), r = e.mapDesc(n.changes, !0), o = n.mapped ? n.mapped.composeDesc(r) : r;
  return new me(s, W.mapEffects(n.effects, e), o, n.startSelection.map(r), i);
}
const Up = /^(input\.type|delete)($|\.)/;
class Ye {
  constructor(e, t, i = 0, s = void 0) {
    this.done = e, this.undone = t, this.prevTime = i, this.prevUserEvent = s;
  }
  isolate() {
    return this.prevTime ? new Ye(this.done, this.undone) : this;
  }
  addChanges(e, t, i, s, r) {
    let o = this.done, l = o[o.length - 1];
    return l && l.changes && !l.changes.empty && e.changes && (!i || Up.test(i)) && (!l.selectionsAfter.length && t - this.prevTime < s.newGroupDelay && s.joinToEvent(r, Vp(l.changes, e.changes)) || // For compose (but not compose.start) events, always join with previous event
    i == "input.type.compose") ? o = Fn(o, o.length - 1, s.minDepth, new me(e.changes.compose(l.changes), Rh(W.mapEffects(e.effects, l.changes), l.effects), l.mapped, l.startSelection, De)) : o = Fn(o, o.length, s.minDepth, e), new Ye(o, De, t, i);
  }
  addSelection(e, t, i, s) {
    let r = this.done.length ? this.done[this.done.length - 1].selectionsAfter : De;
    return r.length > 0 && t - this.prevTime < s && i == this.prevUserEvent && i && /^select($|\.)/.test(i) && Fp(r[r.length - 1], e) ? this : new Ye(Bh(this.done, e), this.undone, t, i);
  }
  addMapping(e) {
    return new Ye(Os(this.done, e), Os(this.undone, e), this.prevTime, this.prevUserEvent);
  }
  pop(e, t, i) {
    let s = e == 0 ? this.done : this.undone;
    if (s.length == 0)
      return null;
    let r = s[s.length - 1], o = r.selectionsAfter[0] || (r.startSelection ? r.startSelection.map(r.changes.invertedDesc, 1) : t.selection);
    if (i && r.selectionsAfter.length)
      return t.update({
        selection: r.selectionsAfter[r.selectionsAfter.length - 1],
        annotations: br.of({ side: e, rest: _p(s), selection: o }),
        userEvent: e == 0 ? "select.undo" : "select.redo",
        scrollIntoView: !0
      });
    if (r.changes) {
      let l = s.length == 1 ? De : s.slice(0, s.length - 1);
      return r.mapped && (l = Os(l, r.mapped)), t.update({
        changes: r.changes,
        selection: r.startSelection,
        effects: r.effects,
        annotations: br.of({ side: e, rest: l, selection: o }),
        filter: !1,
        userEvent: e == 0 ? "undo" : "redo",
        scrollIntoView: !0
      });
    } else
      return null;
  }
}
Ye.empty = /* @__PURE__ */ new Ye(De, De);
const jp = [
  { key: "Mod-z", run: Lh, preventDefault: !0 },
  { key: "Mod-y", mac: "Mod-Shift-z", run: yr, preventDefault: !0 },
  { linux: "Ctrl-Shift-z", run: yr, preventDefault: !0 },
  { key: "Mod-u", run: Np, preventDefault: !0 },
  { key: "Alt-u", mac: "Mod-Shift-u", run: Hp, preventDefault: !0 }
];
function ai(n, e) {
  return y.create(n.ranges.map(e), n.mainIndex);
}
function Ne(n, e) {
  return n.update({ selection: e, scrollIntoView: !0, userEvent: "select" });
}
function He({ state: n, dispatch: e }, t) {
  let i = ai(n.selection, t);
  return i.eq(n.selection, !0) ? !1 : (e(Ne(n, i)), !0);
}
function es(n, e) {
  return y.cursor(e ? n.to : n.from);
}
function Ph(n, e) {
  return He(n, (t) => t.empty ? n.moveByChar(t, e) : es(t, e));
}
function he(n) {
  return n.textDirectionAt(n.state.selection.main.head) == q.LTR;
}
const Ih = (n) => Ph(n, !he(n)), $h = (n) => Ph(n, he(n));
function Nh(n, e) {
  return He(n, (t) => t.empty ? n.moveByGroup(t, e) : es(t, e));
}
const qp = (n) => Nh(n, !he(n)), Kp = (n) => Nh(n, he(n));
function Gp(n, e, t) {
  if (e.type.prop(t))
    return !0;
  let i = e.to - e.from;
  return i && (i > 2 || /[^\s,.;:]/.test(n.sliceDoc(e.from, e.to))) || e.firstChild;
}
function ts(n, e, t) {
  let i = Ze(n).resolveInner(e.head), s = t ? I.closedBy : I.openedBy;
  for (let a = e.head; ; ) {
    let h = t ? i.childAfter(a) : i.childBefore(a);
    if (!h)
      break;
    Gp(n, h, s) ? i = h : a = t ? h.to : h.from;
  }
  let r = i.type.prop(s), o, l;
  return r && (o = t ? Ut(n, i.from, 1) : Ut(n, i.to, -1)) && o.matched ? l = t ? o.end.to : o.end.from : l = t ? i.to : i.from, y.cursor(l, t ? -1 : 1);
}
const Jp = (n) => He(n, (e) => ts(n.state, e, !he(n))), Yp = (n) => He(n, (e) => ts(n.state, e, he(n)));
function Hh(n, e) {
  return He(n, (t) => {
    if (!t.empty)
      return es(t, e);
    let i = n.moveVertically(t, e);
    return i.head != t.head ? i : n.moveToLineBoundary(t, e);
  });
}
const Vh = (n) => Hh(n, !1), Fh = (n) => Hh(n, !0);
function Wh(n) {
  let e = n.scrollDOM.clientHeight < n.scrollDOM.scrollHeight - 2, t = 0, i = 0, s;
  if (e) {
    for (let r of n.state.facet(T.scrollMargins)) {
      let o = r(n);
      o?.top && (t = Math.max(o?.top, t)), o?.bottom && (i = Math.max(o?.bottom, i));
    }
    s = n.scrollDOM.clientHeight - t - i;
  } else
    s = (n.dom.ownerDocument.defaultView || window).innerHeight;
  return {
    marginTop: t,
    marginBottom: i,
    selfScroll: e,
    height: Math.max(n.defaultLineHeight, s - 5)
  };
}
function _h(n, e) {
  let t = Wh(n), { state: i } = n, s = ai(i.selection, (o) => o.empty ? n.moveVertically(o, e, t.height) : es(o, e));
  if (s.eq(i.selection))
    return !1;
  let r;
  if (t.selfScroll) {
    let o = n.coordsAtPos(i.selection.main.head), l = n.scrollDOM.getBoundingClientRect(), a = l.top + t.marginTop, h = l.bottom - t.marginBottom;
    o && o.top > a && o.bottom < h && (r = T.scrollIntoView(s.main.head, { y: "start", yMargin: o.top - a }));
  }
  return n.dispatch(Ne(i, s), { effects: r }), !0;
}
const vl = (n) => _h(n, !1), wr = (n) => _h(n, !0);
function wt(n, e, t) {
  let i = n.lineBlockAt(e.head), s = n.moveToLineBoundary(e, t);
  if (s.head == e.head && s.head != (t ? i.to : i.from) && (s = n.moveToLineBoundary(e, t, !1)), !t && s.head == i.from && i.length) {
    let r = /^\s*/.exec(n.state.sliceDoc(i.from, Math.min(i.from + 100, i.to)))[0].length;
    r && e.head != i.from + r && (s = y.cursor(i.from + r));
  }
  return s;
}
const Xp = (n) => He(n, (e) => wt(n, e, !0)), Zp = (n) => He(n, (e) => wt(n, e, !1)), Qp = (n) => He(n, (e) => wt(n, e, !he(n))), eg = (n) => He(n, (e) => wt(n, e, he(n))), tg = (n) => He(n, (e) => y.cursor(n.lineBlockAt(e.head).from, 1)), ig = (n) => He(n, (e) => y.cursor(n.lineBlockAt(e.head).to, -1));
function ng(n, e, t) {
  let i = !1, s = ai(n.selection, (r) => {
    let o = Ut(n, r.head, -1) || Ut(n, r.head, 1) || r.head > 0 && Ut(n, r.head - 1, 1) || r.head < n.doc.length && Ut(n, r.head + 1, -1);
    if (!o || !o.end)
      return r;
    i = !0;
    let l = o.start.from == r.head ? o.end.to : o.end.from;
    return y.cursor(l);
  });
  return i ? (e(Ne(n, s)), !0) : !1;
}
const sg = ({ state: n, dispatch: e }) => ng(n, e);
function Le(n, e) {
  let t = ai(n.state.selection, (i) => {
    let s = e(i);
    return y.range(i.anchor, s.head, s.goalColumn, s.bidiLevel || void 0, s.assoc);
  });
  return t.eq(n.state.selection) ? !1 : (n.dispatch(Ne(n.state, t)), !0);
}
function zh(n, e) {
  return Le(n, (t) => n.moveByChar(t, e));
}
const Uh = (n) => zh(n, !he(n)), jh = (n) => zh(n, he(n));
function qh(n, e) {
  return Le(n, (t) => n.moveByGroup(t, e));
}
const rg = (n) => qh(n, !he(n)), og = (n) => qh(n, he(n)), lg = (n) => Le(n, (e) => ts(n.state, e, !he(n))), ag = (n) => Le(n, (e) => ts(n.state, e, he(n)));
function Kh(n, e) {
  return Le(n, (t) => n.moveVertically(t, e));
}
const Gh = (n) => Kh(n, !1), Jh = (n) => Kh(n, !0);
function Yh(n, e) {
  return Le(n, (t) => n.moveVertically(t, e, Wh(n).height));
}
const xl = (n) => Yh(n, !1), kl = (n) => Yh(n, !0), hg = (n) => Le(n, (e) => wt(n, e, !0)), cg = (n) => Le(n, (e) => wt(n, e, !1)), fg = (n) => Le(n, (e) => wt(n, e, !he(n))), ug = (n) => Le(n, (e) => wt(n, e, he(n))), dg = (n) => Le(n, (e) => y.cursor(n.lineBlockAt(e.head).from)), pg = (n) => Le(n, (e) => y.cursor(n.lineBlockAt(e.head).to)), Sl = ({ state: n, dispatch: e }) => (e(Ne(n, { anchor: 0 })), !0), Al = ({ state: n, dispatch: e }) => (e(Ne(n, { anchor: n.doc.length })), !0), Cl = ({ state: n, dispatch: e }) => (e(Ne(n, { anchor: n.selection.main.anchor, head: 0 })), !0), Ml = ({ state: n, dispatch: e }) => (e(Ne(n, { anchor: n.selection.main.anchor, head: n.doc.length })), !0), gg = ({ state: n, dispatch: e }) => (e(n.update({ selection: { anchor: 0, head: n.doc.length }, userEvent: "select" })), !0), mg = ({ state: n, dispatch: e }) => {
  let t = is(n).map(({ from: i, to: s }) => y.range(i, Math.min(s + 1, n.doc.length)));
  return e(n.update({ selection: y.create(t), userEvent: "select" })), !0;
}, bg = ({ state: n, dispatch: e }) => {
  let t = ai(n.selection, (i) => {
    let s = Ze(n), r = s.resolveStack(i.from, 1);
    if (i.empty) {
      let o = s.resolveStack(i.from, -1);
      o.node.from >= r.node.from && o.node.to <= r.node.to && (r = o);
    }
    for (let o = r; o; o = o.next) {
      let { node: l } = o;
      if ((l.from < i.from && l.to >= i.to || l.to > i.to && l.from <= i.from) && o.next)
        return y.range(l.to, l.from);
    }
    return i;
  });
  return t.eq(n.selection) ? !1 : (e(Ne(n, t)), !0);
};
function Xh(n, e) {
  let { state: t } = n, i = t.selection, s = t.selection.ranges.slice();
  for (let r of t.selection.ranges) {
    let o = t.doc.lineAt(r.head);
    if (e ? o.to < n.state.doc.length : o.from > 0)
      for (let l = r; ; ) {
        let a = n.moveVertically(l, e);
        if (a.head < o.from || a.head > o.to) {
          s.some((h) => h.head == a.head) || s.push(a);
          break;
        } else {
          if (a.head == l.head)
            break;
          l = a;
        }
      }
  }
  return s.length == i.ranges.length ? !1 : (n.dispatch(Ne(t, y.create(s, s.length - 1))), !0);
}
const yg = (n) => Xh(n, !1), wg = (n) => Xh(n, !0), vg = ({ state: n, dispatch: e }) => {
  let t = n.selection, i = null;
  return t.ranges.length > 1 ? i = y.create([t.main]) : t.main.empty || (i = y.create([y.cursor(t.main.head)])), i ? (e(Ne(n, i)), !0) : !1;
};
function Ki(n, e) {
  if (n.state.readOnly)
    return !1;
  let t = "delete.selection", { state: i } = n, s = i.changeByRange((r) => {
    let { from: o, to: l } = r;
    if (o == l) {
      let a = e(r);
      a < o ? (t = "delete.backward", a = gn(n, a, !1)) : a > o && (t = "delete.forward", a = gn(n, a, !0)), o = Math.min(o, a), l = Math.max(l, a);
    } else
      o = gn(n, o, !1), l = gn(n, l, !0);
    return o == l ? { range: r } : { changes: { from: o, to: l }, range: y.cursor(o, o < r.head ? -1 : 1) };
  });
  return s.changes.empty ? !1 : (n.dispatch(i.update(s, {
    scrollIntoView: !0,
    userEvent: t,
    effects: t == "delete.selection" ? T.announce.of(i.phrase("Selection deleted")) : void 0
  })), !0);
}
function gn(n, e, t) {
  if (n instanceof T)
    for (let i of n.state.facet(T.atomicRanges).map((s) => s(n)))
      i.between(e, e, (s, r) => {
        s < e && r > e && (e = t ? r : s);
      });
  return e;
}
const Zh = (n, e, t) => Ki(n, (i) => {
  let s = i.from, { state: r } = n, o = r.doc.lineAt(s), l, a;
  if (t && !e && s > o.from && s < o.from + 200 && !/[^ \t]/.test(l = o.text.slice(0, s - o.from))) {
    if (l[l.length - 1] == "	")
      return s - 1;
    let h = Un(l, r.tabSize), c = h % $t(r) || $t(r);
    for (let f = 0; f < c && l[l.length - 1 - f] == " "; f++)
      s--;
    a = s;
  } else
    a = ae(o.text, s - o.from, e, e) + o.from, a == s && o.number != (e ? r.doc.lines : 1) ? a += e ? 1 : -1 : !e && /[\ufe00-\ufe0f]/.test(o.text.slice(a - o.from, s - o.from)) && (a = ae(o.text, a - o.from, !1, !1) + o.from);
  return a;
}), vr = (n) => Zh(n, !1, !0), Qh = (n) => Zh(n, !0, !1), ec = (n, e) => Ki(n, (t) => {
  let i = t.head, { state: s } = n, r = s.doc.lineAt(i), o = s.charCategorizer(i);
  for (let l = null; ; ) {
    if (i == (e ? r.to : r.from)) {
      i == t.head && r.number != (e ? s.doc.lines : 1) && (i += e ? 1 : -1);
      break;
    }
    let a = ae(r.text, i - r.from, e) + r.from, h = r.text.slice(Math.min(i, a) - r.from, Math.max(i, a) - r.from), c = o(h);
    if (l != null && c != l)
      break;
    (h != " " || i != t.head) && (l = c), i = a;
  }
  return i;
}), tc = (n) => ec(n, !1), xg = (n) => ec(n, !0), kg = (n) => Ki(n, (e) => {
  let t = n.lineBlockAt(e.head).to;
  return e.head < t ? t : Math.min(n.state.doc.length, e.head + 1);
}), Sg = (n) => Ki(n, (e) => {
  let t = n.moveToLineBoundary(e, !1).head;
  return e.head > t ? t : Math.max(0, e.head - 1);
}), Ag = (n) => Ki(n, (e) => {
  let t = n.moveToLineBoundary(e, !0).head;
  return e.head < t ? t : Math.min(n.state.doc.length, e.head + 1);
}), Cg = ({ state: n, dispatch: e }) => {
  if (n.readOnly)
    return !1;
  let t = n.changeByRange((i) => ({
    changes: { from: i.from, to: i.to, insert: $.of(["", ""]) },
    range: y.cursor(i.from)
  }));
  return e(n.update(t, { scrollIntoView: !0, userEvent: "input" })), !0;
}, Mg = ({ state: n, dispatch: e }) => {
  if (n.readOnly)
    return !1;
  let t = n.changeByRange((i) => {
    if (!i.empty || i.from == 0 || i.from == n.doc.length)
      return { range: i };
    let s = i.from, r = n.doc.lineAt(s), o = s == r.from ? s - 1 : ae(r.text, s - r.from, !1) + r.from, l = s == r.to ? s + 1 : ae(r.text, s - r.from, !0) + r.from;
    return {
      changes: { from: o, to: l, insert: n.doc.slice(s, l).append(n.doc.slice(o, s)) },
      range: y.cursor(l)
    };
  });
  return t.changes.empty ? !1 : (e(n.update(t, { scrollIntoView: !0, userEvent: "move.character" })), !0);
};
function is(n) {
  let e = [], t = -1;
  for (let i of n.selection.ranges) {
    let s = n.doc.lineAt(i.from), r = n.doc.lineAt(i.to);
    if (!i.empty && i.to == r.from && (r = n.doc.lineAt(i.to - 1)), t >= s.number) {
      let o = e[e.length - 1];
      o.to = r.to, o.ranges.push(i);
    } else
      e.push({ from: s.from, to: r.to, ranges: [i] });
    t = r.number + 1;
  }
  return e;
}
function ic(n, e, t) {
  if (n.readOnly)
    return !1;
  let i = [], s = [];
  for (let r of is(n)) {
    if (t ? r.to == n.doc.length : r.from == 0)
      continue;
    let o = n.doc.lineAt(t ? r.to + 1 : r.from - 1), l = o.length + 1;
    if (t) {
      i.push({ from: r.to, to: o.to }, { from: r.from, insert: o.text + n.lineBreak });
      for (let a of r.ranges)
        s.push(y.range(Math.min(n.doc.length, a.anchor + l), Math.min(n.doc.length, a.head + l)));
    } else {
      i.push({ from: o.from, to: r.from }, { from: r.to, insert: n.lineBreak + o.text });
      for (let a of r.ranges)
        s.push(y.range(a.anchor - l, a.head - l));
    }
  }
  return i.length ? (e(n.update({
    changes: i,
    scrollIntoView: !0,
    selection: y.create(s, n.selection.mainIndex),
    userEvent: "move.line"
  })), !0) : !1;
}
const Tg = ({ state: n, dispatch: e }) => ic(n, e, !1), Og = ({ state: n, dispatch: e }) => ic(n, e, !0);
function nc(n, e, t) {
  if (n.readOnly)
    return !1;
  let i = [];
  for (let r of is(n))
    t ? i.push({ from: r.from, insert: n.doc.slice(r.from, r.to) + n.lineBreak }) : i.push({ from: r.to, insert: n.lineBreak + n.doc.slice(r.from, r.to) });
  let s = n.changes(i);
  return e(n.update({
    changes: s,
    selection: n.selection.map(s, t ? 1 : -1),
    scrollIntoView: !0,
    userEvent: "input.copyline"
  })), !0;
}
const Dg = ({ state: n, dispatch: e }) => nc(n, e, !1), Eg = ({ state: n, dispatch: e }) => nc(n, e, !0), Lg = (n) => {
  if (n.state.readOnly)
    return !1;
  let { state: e } = n, t = e.changes(is(e).map(({ from: s, to: r }) => (s > 0 ? s-- : r < e.doc.length && r++, { from: s, to: r }))), i = ai(e.selection, (s) => {
    let r;
    if (n.lineWrapping) {
      let o = n.lineBlockAt(s.head), l = n.coordsAtPos(s.head, s.assoc || 1);
      l && (r = o.bottom + n.documentTop - l.bottom + n.defaultLineHeight / 2);
    }
    return n.moveVertically(s, !0, r);
  }).map(t);
  return n.dispatch({ changes: t, selection: i, scrollIntoView: !0, userEvent: "delete.line" }), !0;
};
function Rg(n, e) {
  if (/\(\)|\[\]|\{\}/.test(n.sliceDoc(e - 1, e + 1)))
    return { from: e, to: e };
  let t = Ze(n).resolveInner(e), i = t.childBefore(e), s = t.childAfter(e), r;
  return i && s && i.to <= e && s.from >= e && (r = i.type.prop(I.closedBy)) && r.indexOf(s.name) > -1 && n.doc.lineAt(i.to).from == n.doc.lineAt(s.from).from && !/\S/.test(n.sliceDoc(i.to, s.from)) ? { from: i.to, to: s.from } : null;
}
const Tl = /* @__PURE__ */ sc(!1), Bg = /* @__PURE__ */ sc(!0);
function sc(n) {
  return ({ state: e, dispatch: t }) => {
    if (e.readOnly)
      return !1;
    let i = e.changeByRange((s) => {
      let { from: r, to: o } = s, l = e.doc.lineAt(r), a = !n && r == o && Rg(e, r);
      n && (r = o = (o <= l.to ? l : e.doc.lineAt(o)).to);
      let h = new Xn(e, { simulateBreak: r, simulateDoubleBreak: !!a }), c = wh(h, r);
      for (c == null && (c = Un(/^\s*/.exec(e.doc.lineAt(r).text)[0], e.tabSize)); o < l.to && /\s/.test(l.text[o - l.from]); )
        o++;
      a ? { from: r, to: o } = a : r > l.from && r < l.from + 100 && !/\S/.test(l.text.slice(0, r)) && (r = l.from);
      let f = ["", Vn(e, c)];
      return a && f.push(Vn(e, h.lineIndent(l.from, -1))), {
        changes: { from: r, to: o, insert: $.of(f) },
        range: y.cursor(r + 1 + f[1].length)
      };
    });
    return t(e.update(i, { scrollIntoView: !0, userEvent: "input" })), !0;
  };
}
function Yr(n, e) {
  let t = -1;
  return n.changeByRange((i) => {
    let s = [];
    for (let o = i.from; o <= i.to; ) {
      let l = n.doc.lineAt(o);
      l.number > t && (i.empty || i.to > l.from) && (e(l, s, i), t = l.number), o = l.to + 1;
    }
    let r = n.changes(s);
    return {
      changes: s,
      range: y.range(r.mapPos(i.anchor, 1), r.mapPos(i.head, 1))
    };
  });
}
const Pg = ({ state: n, dispatch: e }) => {
  if (n.readOnly)
    return !1;
  let t = /* @__PURE__ */ Object.create(null), i = new Xn(n, { overrideIndentation: (r) => {
    let o = t[r];
    return o ?? -1;
  } }), s = Yr(n, (r, o, l) => {
    let a = wh(i, r.from);
    if (a == null)
      return;
    /\S/.test(r.text) || (a = 0);
    let h = /^\s*/.exec(r.text)[0], c = Vn(n, a);
    (h != c || l.from < r.from + h.length) && (t[r.from] = a, o.push({ from: r.from, to: r.from + h.length, insert: c }));
  });
  return s.changes.empty || e(n.update(s, { userEvent: "indent" })), !0;
}, rc = ({ state: n, dispatch: e }) => n.readOnly ? !1 : (e(n.update(Yr(n, (t, i) => {
  i.push({ from: t.from, insert: n.facet(zr) });
}), { userEvent: "input.indent" })), !0), oc = ({ state: n, dispatch: e }) => n.readOnly ? !1 : (e(n.update(Yr(n, (t, i) => {
  let s = /^\s*/.exec(t.text)[0];
  if (!s)
    return;
  let r = Un(s, n.tabSize), o = 0, l = Vn(n, Math.max(0, r - $t(n)));
  for (; o < s.length && o < l.length && s.charCodeAt(o) == l.charCodeAt(o); )
    o++;
  i.push({ from: t.from + o, to: t.from + s.length, insert: l.slice(o) });
}), { userEvent: "delete.dedent" })), !0), Ig = (n) => (n.setTabFocusMode(), !0), $g = [
  { key: "Ctrl-b", run: Ih, shift: Uh, preventDefault: !0 },
  { key: "Ctrl-f", run: $h, shift: jh },
  { key: "Ctrl-p", run: Vh, shift: Gh },
  { key: "Ctrl-n", run: Fh, shift: Jh },
  { key: "Ctrl-a", run: tg, shift: dg },
  { key: "Ctrl-e", run: ig, shift: pg },
  { key: "Ctrl-d", run: Qh },
  { key: "Ctrl-h", run: vr },
  { key: "Ctrl-k", run: kg },
  { key: "Ctrl-Alt-h", run: tc },
  { key: "Ctrl-o", run: Cg },
  { key: "Ctrl-t", run: Mg },
  { key: "Ctrl-v", run: wr }
], Ng = /* @__PURE__ */ [
  { key: "ArrowLeft", run: Ih, shift: Uh, preventDefault: !0 },
  { key: "Mod-ArrowLeft", mac: "Alt-ArrowLeft", run: qp, shift: rg, preventDefault: !0 },
  { mac: "Cmd-ArrowLeft", run: Qp, shift: fg, preventDefault: !0 },
  { key: "ArrowRight", run: $h, shift: jh, preventDefault: !0 },
  { key: "Mod-ArrowRight", mac: "Alt-ArrowRight", run: Kp, shift: og, preventDefault: !0 },
  { mac: "Cmd-ArrowRight", run: eg, shift: ug, preventDefault: !0 },
  { key: "ArrowUp", run: Vh, shift: Gh, preventDefault: !0 },
  { mac: "Cmd-ArrowUp", run: Sl, shift: Cl },
  { mac: "Ctrl-ArrowUp", run: vl, shift: xl },
  { key: "ArrowDown", run: Fh, shift: Jh, preventDefault: !0 },
  { mac: "Cmd-ArrowDown", run: Al, shift: Ml },
  { mac: "Ctrl-ArrowDown", run: wr, shift: kl },
  { key: "PageUp", run: vl, shift: xl },
  { key: "PageDown", run: wr, shift: kl },
  { key: "Home", run: Zp, shift: cg, preventDefault: !0 },
  { key: "Mod-Home", run: Sl, shift: Cl },
  { key: "End", run: Xp, shift: hg, preventDefault: !0 },
  { key: "Mod-End", run: Al, shift: Ml },
  { key: "Enter", run: Tl, shift: Tl },
  { key: "Mod-a", run: gg },
  { key: "Backspace", run: vr, shift: vr, preventDefault: !0 },
  { key: "Delete", run: Qh, preventDefault: !0 },
  { key: "Mod-Backspace", mac: "Alt-Backspace", run: tc, preventDefault: !0 },
  { key: "Mod-Delete", mac: "Alt-Delete", run: xg, preventDefault: !0 },
  { mac: "Mod-Backspace", run: Sg, preventDefault: !0 },
  { mac: "Mod-Delete", run: Ag, preventDefault: !0 }
].concat(/* @__PURE__ */ $g.map((n) => ({ mac: n.key, run: n.run, shift: n.shift }))), Hg = /* @__PURE__ */ [
  { key: "Alt-ArrowLeft", mac: "Ctrl-ArrowLeft", run: Jp, shift: lg },
  { key: "Alt-ArrowRight", mac: "Ctrl-ArrowRight", run: Yp, shift: ag },
  { key: "Alt-ArrowUp", run: Tg },
  { key: "Shift-Alt-ArrowUp", run: Dg },
  { key: "Alt-ArrowDown", run: Og },
  { key: "Shift-Alt-ArrowDown", run: Eg },
  { key: "Mod-Alt-ArrowUp", run: yg },
  { key: "Mod-Alt-ArrowDown", run: wg },
  { key: "Escape", run: vg },
  { key: "Mod-Enter", run: Bg },
  { key: "Alt-l", mac: "Ctrl-l", run: mg },
  { key: "Mod-i", run: bg, preventDefault: !0 },
  { key: "Mod-[", run: oc },
  { key: "Mod-]", run: rc },
  { key: "Mod-Alt-\\", run: Pg },
  { key: "Shift-Mod-k", run: Lg },
  { key: "Shift-Mod-\\", run: sg },
  { key: "Mod-/", run: Tp },
  { key: "Alt-A", run: Dp },
  { key: "Ctrl-m", mac: "Shift-Alt-m", run: Ig }
].concat(Ng), Vg = { key: "Tab", run: rc, shift: oc };
class Ol {
  constructor(e, t, i) {
    this.from = e, this.to = t, this.diagnostic = i;
  }
}
class Ct {
  constructor(e, t, i) {
    this.diagnostics = e, this.panel = t, this.selected = i;
  }
  static init(e, t, i) {
    let s = i.facet(Ni).markerFilter;
    s && (e = s(e, i));
    let r = e.slice().sort((d, p) => d.from - p.from || d.to - p.to), o = new Xt(), l = [], a = 0, h = i.doc.iter(), c = 0, f = i.doc.length;
    for (let d = 0; ; ) {
      let p = d == r.length ? null : r[d];
      if (!p && !l.length)
        break;
      let g, m;
      if (l.length)
        g = a, m = l.reduce((w, O) => Math.min(w, O.to), p && p.from > g ? p.from : 1e8);
      else {
        if (g = p.from, g > f)
          break;
        m = p.to, l.push(p), d++;
      }
      for (; d < r.length; ) {
        let w = r[d];
        if (w.from == g && (w.to > w.from || w.to == g))
          l.push(w), d++, m = Math.min(w.to, m);
        else {
          m = Math.min(w.from, m);
          break;
        }
      }
      m = Math.min(m, f);
      let b = !1;
      if (l.some((w) => w.from == g && (w.to == m || m == f)) && (b = g == m, !b && m - g < 10)) {
        let w = g - (c + h.value.length);
        w > 0 && (h.next(w), c = g);
        for (let O = g; ; ) {
          if (O >= m) {
            b = !0;
            break;
          }
          if (!h.lineBreak && c + h.value.length > O)
            break;
          O = c + h.value.length, c += h.value.length, h.next();
        }
      }
      let v = dc(l);
      if (b)
        o.add(g, g, K.widget({
          widget: new qg(v),
          diagnostics: l.slice()
        }));
      else {
        let w = l.reduce((O, k) => k.markClass ? O + " " + k.markClass : O, "");
        o.add(g, m, K.mark({
          class: "cm-lintRange cm-lintRange-" + v + w,
          diagnostics: l.slice(),
          inclusiveEnd: l.some((O) => O.to > m)
        }));
      }
      if (a = m, a == f)
        break;
      for (let w = 0; w < l.length; w++)
        l[w].to <= a && l.splice(w--, 1);
    }
    let u = o.finish();
    return new Ct(u, t, bt(u));
  }
}
function bt(n, e = null, t = 0) {
  let i = null;
  return n.between(t, 1e9, (s, r, { spec: o }) => {
    if (!(e && o.diagnostics.indexOf(e) < 0))
      if (!i)
        i = new Ol(s, r, e || o.diagnostics[0]);
      else {
        if (o.diagnostics.indexOf(i.diagnostic) < 0)
          return !1;
        i = new Ol(i.from, r, i.diagnostic);
      }
  }), i;
}
function lc(n, e) {
  let t = e.pos, i = e.end || t, s = n.state.facet(Ni).hideOn(n, t, i);
  if (s != null)
    return s;
  let r = n.startState.doc.lineAt(e.pos);
  return !!(n.effects.some((o) => o.is(ns)) || n.changes.touchesRange(r.from, Math.max(r.to, i)));
}
function ac(n, e) {
  return n.field(Se, !1) ? e : e.concat(W.appendConfig.of(em));
}
function Fg(n, e) {
  return {
    effects: ac(n, [ns.of(e)])
  };
}
const ns = /* @__PURE__ */ W.define(), Xr = /* @__PURE__ */ W.define(), hc = /* @__PURE__ */ W.define(), Se = /* @__PURE__ */ Ae.define({
  create() {
    return new Ct(K.none, null, null);
  },
  update(n, e) {
    if (e.docChanged && n.diagnostics.size) {
      let t = n.diagnostics.map(e.changes), i = null, s = n.panel;
      if (n.selected) {
        let r = e.changes.mapPos(n.selected.from, 1);
        i = bt(t, n.selected.diagnostic, r) || bt(t, null, r);
      }
      !t.size && s && e.state.facet(Ni).autoPanel && (s = null), n = new Ct(t, s, i);
    }
    for (let t of e.effects)
      if (t.is(ns)) {
        let i = e.state.facet(Ni).autoPanel ? t.value.length ? Hi.open : null : n.panel;
        n = Ct.init(t.value, i, e.state);
      } else t.is(Xr) ? n = new Ct(n.diagnostics, t.value ? Hi.open : null, n.selected) : t.is(hc) && (n = new Ct(n.diagnostics, n.panel, t.value));
    return n;
  },
  provide: (n) => [
    hr.from(n, (e) => e.panel),
    T.decorations.from(n, (e) => e.diagnostics)
  ]
}), Wg = /* @__PURE__ */ K.mark({ class: "cm-lintRange cm-lintRange-active" });
function _g(n, e, t) {
  let { diagnostics: i } = n.state.field(Se), s, r = -1, o = -1;
  i.between(e - (t < 0 ? 1 : 0), e + (t > 0 ? 1 : 0), (a, h, { spec: c }) => {
    if (e >= a && e <= h && (a == h || (e > a || t > 0) && (e < h || t < 0)))
      return s = c.diagnostics, r = a, o = h, !1;
  });
  let l = n.state.facet(Ni).tooltipFilter;
  return s && l && (s = l(s, n.state)), s ? {
    pos: r,
    end: o,
    above: n.state.doc.lineAt(r).to < o,
    create() {
      return { dom: cc(n, s) };
    }
  } : null;
}
function cc(n, e) {
  return Ke("ul", { class: "cm-tooltip-lint" }, e.map((t) => uc(n, t, !1)));
}
const zg = (n) => {
  let e = n.state.field(Se, !1);
  (!e || !e.panel) && n.dispatch({ effects: ac(n.state, [Xr.of(!0)]) });
  let t = kd(n, Hi.open);
  return t && t.dom.querySelector(".cm-panel-lint ul").focus(), !0;
}, Dl = (n) => {
  let e = n.state.field(Se, !1);
  return !e || !e.panel ? !1 : (n.dispatch({ effects: Xr.of(!1) }), !0);
}, Ug = (n) => {
  let e = n.state.field(Se, !1);
  if (!e)
    return !1;
  let t = n.state.selection.main, i = bt(e.diagnostics, null, t.to + 1);
  return !i && (i = bt(e.diagnostics, null, 0), !i || i.from == t.from && i.to == t.to) ? !1 : (n.dispatch({ selection: { anchor: i.from, head: i.to }, scrollIntoView: !0 }), vd(n, i.from, 1, {
    tooltip: mc,
    until: (s) => s.docChanged || s.newSelection.main.head < i.from || s.newSelection.main.head > i.to
  }), !0);
}, jg = [
  { key: "Mod-Shift-m", run: zg, preventDefault: !0 },
  { key: "F8", run: Ug }
], Ni = /* @__PURE__ */ M.define({
  combine(n) {
    return {
      sources: n.map((e) => e.source).filter((e) => e != null),
      ..._i(n.map((e) => e.config), {
        delay: 750,
        markerFilter: null,
        tooltipFilter: null,
        needsRefresh: null,
        hideOn: () => null
      }, {
        delay: Math.max,
        markerFilter: El,
        tooltipFilter: El,
        needsRefresh: (e, t) => e ? t ? (i) => e(i) || t(i) : e : t,
        hideOn: (e, t) => e ? t ? (i, s, r) => e(i, s, r) || t(i, s, r) : e : t,
        autoPanel: (e, t) => e || t
      })
    };
  }
});
function El(n, e) {
  return n ? e ? (t, i) => e(n(t, i), i) : n : e;
}
function fc(n) {
  let e = [];
  if (n)
    e: for (let { name: t } of n) {
      for (let i = 0; i < t.length; i++) {
        let s = t[i];
        if (/[a-zA-Z]/.test(s) && !e.some((r) => r.toLowerCase() == s.toLowerCase())) {
          e.push(s);
          continue e;
        }
      }
      e.push("");
    }
  return e;
}
function uc(n, e, t) {
  var i;
  let s = t ? fc(e.actions) : [];
  return Ke("li", { class: "cm-diagnostic cm-diagnostic-" + e.severity }, Ke("span", { class: "cm-diagnosticText" }, e.renderMessage ? e.renderMessage(n) : e.message), (i = e.actions) === null || i === void 0 ? void 0 : i.map((r, o) => {
    let l = !1, a = (d) => {
      if (d.preventDefault(), l)
        return;
      l = !0;
      let p = bt(n.state.field(Se).diagnostics, e);
      p && r.apply(n, p.from, p.to);
    }, { name: h } = r, c = s[o] ? h.indexOf(s[o]) : -1, f = c < 0 ? h : [
      h.slice(0, c),
      Ke("u", h.slice(c, c + 1)),
      h.slice(c + 1)
    ], u = r.markClass ? " " + r.markClass : "";
    return Ke("button", {
      type: "button",
      class: "cm-diagnosticAction" + u,
      onclick: a,
      onmousedown: a,
      "aria-label": ` Action: ${h}${c < 0 ? "" : ` (access key "${s[o]})"`}.`
    }, f);
  }), e.source && Ke("div", { class: "cm-diagnosticSource" }, e.source));
}
class qg extends zi {
  constructor(e) {
    super(), this.sev = e;
  }
  eq(e) {
    return e.sev == this.sev;
  }
  toDOM() {
    return Ke("span", { class: "cm-lintPoint cm-lintPoint-" + this.sev });
  }
}
class Ll {
  constructor(e, t) {
    this.diagnostic = t, this.id = "item_" + Math.floor(Math.random() * 4294967295).toString(16), this.dom = uc(e, t, !0), this.dom.id = this.id, this.dom.setAttribute("role", "option");
  }
}
class Hi {
  constructor(e) {
    this.view = e, this.items = [];
    let t = (s) => {
      if (!(s.ctrlKey || s.altKey || s.metaKey)) {
        if (s.keyCode == 27)
          Dl(this.view), this.view.focus();
        else if (s.keyCode == 38 || s.keyCode == 33)
          this.moveSelection((this.selectedIndex - 1 + this.items.length) % this.items.length);
        else if (s.keyCode == 40 || s.keyCode == 34)
          this.moveSelection((this.selectedIndex + 1) % this.items.length);
        else if (s.keyCode == 36)
          this.moveSelection(0);
        else if (s.keyCode == 35)
          this.moveSelection(this.items.length - 1);
        else if (s.keyCode == 13)
          this.view.focus();
        else if (s.keyCode >= 65 && s.keyCode <= 90 && this.selectedIndex >= 0) {
          let { diagnostic: r } = this.items[this.selectedIndex], o = fc(r.actions);
          for (let l = 0; l < o.length; l++)
            if (o[l].toUpperCase().charCodeAt(0) == s.keyCode) {
              let a = bt(this.view.state.field(Se).diagnostics, r);
              a && r.actions[l].apply(e, a.from, a.to);
            }
        } else
          return;
        s.preventDefault();
      }
    }, i = (s) => {
      for (let r = 0; r < this.items.length; r++)
        this.items[r].dom.contains(s.target) && this.moveSelection(r);
    };
    this.list = Ke("ul", {
      tabIndex: 0,
      role: "listbox",
      "aria-label": this.view.state.phrase("Diagnostics"),
      onkeydown: t,
      onclick: i
    }), this.dom = Ke("div", { class: "cm-panel-lint" }, this.list, Ke("button", {
      type: "button",
      name: "close",
      "aria-label": this.view.state.phrase("close"),
      onclick: () => Dl(this.view)
    }, "×")), this.update();
  }
  get selectedIndex() {
    let e = this.view.state.field(Se).selected;
    if (!e)
      return -1;
    for (let t = 0; t < this.items.length; t++)
      if (this.items[t].diagnostic == e.diagnostic)
        return t;
    return -1;
  }
  update() {
    let { diagnostics: e, selected: t } = this.view.state.field(Se), i = 0, s = !1, r = null, o = /* @__PURE__ */ new Set();
    for (e.between(0, this.view.state.doc.length, (l, a, { spec: h }) => {
      for (let c of h.diagnostics) {
        if (o.has(c))
          continue;
        o.add(c);
        let f = -1, u;
        for (let d = i; d < this.items.length; d++)
          if (this.items[d].diagnostic == c) {
            f = d;
            break;
          }
        f < 0 ? (u = new Ll(this.view, c), this.items.splice(i, 0, u), s = !0) : (u = this.items[f], f > i && (this.items.splice(i, f - i), s = !0)), t && u.diagnostic == t.diagnostic ? u.dom.hasAttribute("aria-selected") || (u.dom.setAttribute("aria-selected", "true"), r = u) : u.dom.hasAttribute("aria-selected") && u.dom.removeAttribute("aria-selected"), i++;
      }
    }); i < this.items.length && !(this.items.length == 1 && this.items[0].diagnostic.from < 0); )
      s = !0, this.items.pop();
    this.items.length == 0 && (this.items.push(new Ll(this.view, {
      from: -1,
      to: -1,
      severity: "info",
      message: this.view.state.phrase("No diagnostics")
    })), s = !0), r ? (this.list.setAttribute("aria-activedescendant", r.id), this.view.requestMeasure({
      key: this,
      read: () => ({ sel: r.dom.getBoundingClientRect(), panel: this.list.getBoundingClientRect() }),
      write: ({ sel: l, panel: a }) => {
        let h = a.height / this.list.offsetHeight;
        l.top < a.top ? this.list.scrollTop -= (a.top - l.top) / h : l.bottom > a.bottom && (this.list.scrollTop += (l.bottom - a.bottom) / h);
      }
    })) : this.selectedIndex < 0 && this.list.removeAttribute("aria-activedescendant"), s && this.sync();
  }
  sync() {
    let e = this.list.firstChild;
    function t() {
      let i = e;
      e = i.nextSibling, i.remove();
    }
    for (let i of this.items)
      if (i.dom.parentNode == this.list) {
        for (; e != i.dom; )
          t();
        e = i.dom.nextSibling;
      } else
        this.list.insertBefore(i.dom, e);
    for (; e; )
      t();
  }
  moveSelection(e) {
    if (this.selectedIndex < 0)
      return;
    let t = this.view.state.field(Se), i = bt(t.diagnostics, this.items[e].diagnostic);
    i && this.view.dispatch({
      selection: { anchor: i.from, head: i.to },
      scrollIntoView: !0,
      effects: hc.of(i)
    });
  }
  static open(e) {
    return new Hi(e);
  }
}
function An(n, e = 'viewBox="0 0 40 40"') {
  return `url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" ${e}>${encodeURIComponent(n)}</svg>')`;
}
function mn(n) {
  return An(`<path d="m0 2.5 l2 -1.5 l1 0 l2 1.5 l1 0" stroke="${n}" fill="none" stroke-width=".7"/>`, 'width="6" height="3"');
}
const Kg = /* @__PURE__ */ T.baseTheme({
  ".cm-diagnostic": {
    padding: "3px 6px 3px 8px",
    marginLeft: "-1px",
    display: "block",
    whiteSpace: "pre-wrap"
  },
  ".cm-diagnostic-error": { borderLeft: "5px solid #d11" },
  ".cm-diagnostic-warning": { borderLeft: "5px solid orange" },
  ".cm-diagnostic-info": { borderLeft: "5px solid #999" },
  ".cm-diagnostic-hint": { borderLeft: "5px solid #66d" },
  ".cm-diagnosticAction": {
    font: "inherit",
    border: "none",
    padding: "2px 4px",
    backgroundColor: "#444",
    color: "white",
    borderRadius: "3px",
    marginLeft: "8px",
    cursor: "pointer"
  },
  ".cm-diagnosticSource": {
    fontSize: "70%",
    opacity: 0.7
  },
  ".cm-lintRange": {
    backgroundPosition: "left bottom",
    backgroundRepeat: "repeat-x",
    paddingBottom: "0.7px"
  },
  ".cm-lintRange-error": { backgroundImage: /* @__PURE__ */ mn("#f11") },
  ".cm-lintRange-warning": { backgroundImage: /* @__PURE__ */ mn("orange") },
  ".cm-lintRange-info": { backgroundImage: /* @__PURE__ */ mn("#999") },
  ".cm-lintRange-hint": { backgroundImage: /* @__PURE__ */ mn("#66d") },
  ".cm-lintRange-active": { backgroundColor: "#ffdd9980" },
  ".cm-tooltip-lint": {
    padding: 0,
    margin: 0
  },
  ".cm-lintPoint": {
    position: "relative",
    "&:after": {
      content: '""',
      position: "absolute",
      bottom: 0,
      left: "-2px",
      borderLeft: "3px solid transparent",
      borderRight: "3px solid transparent",
      borderBottom: "4px solid #d11"
    }
  },
  ".cm-lintPoint-warning": {
    "&:after": { borderBottomColor: "orange" }
  },
  ".cm-lintPoint-info": {
    "&:after": { borderBottomColor: "#999" }
  },
  ".cm-lintPoint-hint": {
    "&:after": { borderBottomColor: "#66d" }
  },
  ".cm-panel.cm-panel-lint": {
    position: "relative",
    "& ul": {
      maxHeight: "100px",
      overflowY: "auto",
      "& [aria-selected]": {
        backgroundColor: "#ddd",
        "& u": { textDecoration: "underline" }
      },
      "&:focus [aria-selected]": {
        background_fallback: "#bdf",
        backgroundColor: "Highlight",
        color_fallback: "white",
        color: "HighlightText"
      },
      "& u": { textDecoration: "none" },
      padding: 0,
      margin: 0
    },
    "& [name=close]": {
      position: "absolute",
      top: "0",
      right: "2px",
      background: "inherit",
      border: "none",
      font: "inherit",
      padding: 0,
      margin: 0
    }
  },
  "&dark .cm-lintRange-active": { backgroundColor: "#86714a80" },
  "&dark .cm-panel.cm-panel-lint ul": {
    "& [aria-selected]": {
      backgroundColor: "#2e343e"
    }
  }
});
function Gg(n) {
  return n == "error" ? 4 : n == "warning" ? 3 : n == "info" ? 2 : 1;
}
function dc(n) {
  let e = "hint", t = 1;
  for (let i of n) {
    let s = Gg(i.severity);
    s > t && (t = s, e = i.severity);
  }
  return e;
}
class pc extends st {
  constructor(e) {
    super(), this.diagnostics = e, this.severity = dc(e);
  }
  toDOM(e) {
    let t = document.createElement("div");
    t.className = "cm-lint-marker cm-lint-marker-" + this.severity;
    let i = this.diagnostics, s = e.state.facet(ss).tooltipFilter;
    return s && (i = s(i, e.state)), i.length && (t.onmouseover = () => Yg(e, t, i)), t;
  }
}
function Jg(n, e) {
  let t = (i) => {
    let s = e.getBoundingClientRect();
    if (!(i.clientX > s.left - 10 && i.clientX < s.right + 10 && i.clientY > s.top - 10 && i.clientY < s.bottom + 10)) {
      for (let r = i.target; r; r = r.parentNode)
        if (r.nodeType == 1 && r.classList.contains("cm-tooltip-lint"))
          return;
      window.removeEventListener("mousemove", t), n.state.field(gc) && n.dispatch({ effects: Zr.of(null) });
    }
  };
  window.addEventListener("mousemove", t);
}
function Yg(n, e, t) {
  function i() {
    let o = n.elementAtHeight(e.getBoundingClientRect().top + 5 - n.documentTop);
    n.coordsAtPos(o.from) && n.dispatch({ effects: Zr.of({
      pos: o.from,
      above: !1,
      clip: !1,
      create() {
        return {
          dom: cc(n, t),
          getCoords: () => e.getBoundingClientRect()
        };
      }
    }) }), e.onmouseout = e.onmousemove = null, Jg(n, e);
  }
  let { hoverTime: s } = n.state.facet(ss), r = setTimeout(i, s);
  e.onmouseout = () => {
    clearTimeout(r), e.onmouseout = e.onmousemove = null;
  }, e.onmousemove = () => {
    clearTimeout(r), r = setTimeout(i, s);
  };
}
function Xg(n, e) {
  let t = /* @__PURE__ */ Object.create(null);
  for (let s of e) {
    let r = n.lineAt(s.from);
    (t[r.from] || (t[r.from] = [])).push(s);
  }
  let i = [];
  for (let s in t)
    i.push(new pc(t[s]).range(+s));
  return B.of(i, !0);
}
const Zg = /* @__PURE__ */ Cd({
  class: "cm-gutter-lint",
  markers: (n) => n.state.field(xr),
  widgetMarker: (n, e, t) => {
    let i = [];
    return n.state.field(xr).between(t.from, t.to, (s, r, o) => {
      s > t.from && s < t.to && i.push(...o.diagnostics);
    }), i.length ? new pc(i) : null;
  }
}), xr = /* @__PURE__ */ Ae.define({
  create() {
    return B.empty;
  },
  update(n, e) {
    n = n.map(e.changes);
    let t = e.state.facet(ss).markerFilter;
    for (let i of e.effects)
      if (i.is(ns)) {
        let s = i.value;
        t && (s = t(s || [], e.state)), n = Xg(e.state.doc, s.slice(0));
      }
    return n;
  }
}), Zr = /* @__PURE__ */ W.define(), gc = /* @__PURE__ */ Ae.define({
  create() {
    return null;
  },
  update(n, e) {
    return n && e.docChanged && (n = lc(e, n) ? null : { ...n, pos: e.changes.mapPos(n.pos) }), e.effects.reduce((t, i) => i.is(Zr) ? i.value : t, n);
  },
  provide: (n) => Hr.from(n)
}), Qg = /* @__PURE__ */ T.baseTheme({
  ".cm-gutter-lint": {
    width: "1.4em",
    "& .cm-gutterElement": {
      padding: ".2em"
    }
  },
  ".cm-lint-marker": {
    width: "1em",
    height: "1em"
  },
  ".cm-lint-marker-info": {
    content: /* @__PURE__ */ An('<path fill="#aaf" stroke="#77e" stroke-width="6" stroke-linejoin="round" d="M5 5L35 5L35 35L5 35Z"/>')
  },
  ".cm-lint-marker-warning": {
    content: /* @__PURE__ */ An('<path fill="#fe8" stroke="#fd7" stroke-width="6" stroke-linejoin="round" d="M20 6L37 35L3 35Z"/>')
  },
  ".cm-lint-marker-error": {
    content: /* @__PURE__ */ An('<circle cx="20" cy="20" r="15" fill="#f87" stroke="#f43" stroke-width="6"/>')
  }
}), mc = /* @__PURE__ */ wd(_g, { hideOn: lc }), em = [
  Se,
  /* @__PURE__ */ T.decorations.compute([Se], (n) => {
    let { selected: e, panel: t } = n.field(Se);
    return !e || !t || e.from == e.to ? K.none : K.set([
      Wg.range(e.from, e.to)
    ]);
  }),
  mc,
  Kg
], ss = /* @__PURE__ */ M.define({
  combine(n) {
    return _i(n, {
      hoverTime: 300,
      markerFilter: null,
      tooltipFilter: null
    });
  }
});
function tm(n = {}) {
  return [ss.of(n), xr, Zg, Qg, gc];
}
const im = /^(INTERLIS|MODEL|TOPIC|CLASS|STRUCTURE|DOMAIN|TYPE|END|IMPORTS|REFSYSTEM|CONTRACTED|VIEW|EXTENDS|ABSTRACT|FINAL|BASKET|DATA|OID|NO|HALIGNMENT|VALIGNMENT|MANDATORY|CONSTRAINTS|EXISTENCE|UNIQUE|SET|BAG|LIST|OF|REFERENCE|TO|ASSOCIATION|ROLE|ATTRIBUTE|TEXT|BOOLEAN|NUMERIC|COORD|SURFACE|AREA|POLYLINE|FORMAT|BASED ON|VERSION|AT)\b/i, nm = jr.define({
  token(n) {
    return n.match(/!!.*/) ? "comment" : n.match(/"([^"\\]|\\.)*"?/) ? "string" : n.match(/\b\d+(\.\d+)?\b/) ? "number" : n.match(im) ? "keyword" : n.match(/[A-Z][A-Za-z0-9_]*/) ? "typeName" : (n.next(), null);
  },
  languageData: {
    commentTokens: { line: "!!" }
  }
}), sm = Zn.define([
  { tag: E.keyword, color: "#075985", fontWeight: "600" },
  { tag: E.comment, color: "#64748b", fontStyle: "italic" },
  { tag: E.string, color: "#047857" },
  { tag: E.number, color: "#b45309" },
  { tag: E.typeName, color: "#7c3aed" }
]), rm = T.theme({
  "&": {
    backgroundColor: "#fbfcfd",
    color: "#172033",
    fontSize: "14px",
    height: "100%"
  },
  ".cm-scroller": {
    fontFamily: "var(--interlis-lab-monospace-font)",
    lineHeight: "1.55",
    minHeight: "22rem"
  },
  ".cm-content": {
    caretColor: "#0f766e",
    padding: "1rem 0.75rem"
  },
  ".cm-line": {
    padding: "0 0.35rem"
  },
  ".cm-gutters": {
    backgroundColor: "#f6f8fb",
    borderRight: "1px solid #e2e8f0",
    color: "#8492a6"
  },
  ".cm-activeLine": {
    backgroundColor: "#eef6f5"
  },
  ".cm-activeLineGutter": {
    backgroundColor: "#eef6f5",
    color: "#334155"
  },
  ".cm-selectionBackground, &.cm-focused .cm-selectionBackground": {
    backgroundColor: "#bfdbfe"
  },
  "&.cm-focused": {
    outline: "2px solid rgba(15, 118, 110, 0.35)",
    outlineOffset: "-2px"
  },
  ".cm-diagnostic": {
    fontFamily: "var(--interlis-lab-font-family)"
  }
});
function om(n, e) {
  if (!n.line)
    return;
  const t = Math.min(Math.max(n.line, 1), e.doc.lines), i = e.doc.line(t), s = n.column ? Math.max(n.column - 1, 0) : 0, r = Math.min(i.from + s, i.to), o = r < i.to ? r + 1 : i.to;
  return {
    from: r,
    to: o,
    severity: n.severity,
    message: n.message,
    source: n.source
  };
}
class lm {
  constructor(e) {
    this.textarea = e, this.readOnly = new Yt(), this.editable = new Yt(), this.syncingFromView = !1, this.fallback = new Fc(e);
    try {
      const t = e.parentElement;
      if (!t)
        return;
      this.view = new T({
        parent: t,
        state: N.create({
          doc: e.value,
          extensions: [
            Rd(),
            Id(),
            $p(),
            nd(),
            ad(),
            hd(),
            nm,
            up(sm),
            tm(),
            th.of([Vg, ...Hg, ...jp, ...jg]),
            T.lineWrapping,
            T.contentAttributes.of({
              "aria-label": e.getAttribute("aria-label") ?? "INTERLIS-Code Editor"
            }),
            T.updateListener.of((i) => {
              if (!(!i.docChanged || this.syncingFromView)) {
                this.syncingFromView = !0;
                try {
                  this.fallback.setValue(i.state.doc.toString()), e.dispatchEvent(new Event("input", { bubbles: !0, composed: !0 }));
                } finally {
                  this.syncingFromView = !1;
                }
              }
            }),
            this.readOnly.of(N.readOnly.of(e.readOnly)),
            this.editable.of(T.editable.of(!e.readOnly)),
            rm
          ]
        })
      }), e.dataset.editor = "codemirror";
    } catch {
      this.view = void 0;
    }
  }
  getValue() {
    return this.view?.state.doc.toString() ?? this.fallback.getValue();
  }
  setValue(e) {
    if (this.fallback.setValue(e), !(!this.view || this.syncingFromView || this.view.state.doc.toString() === e)) {
      this.syncingFromView = !0;
      try {
        this.view.dispatch({
          changes: {
            from: 0,
            to: this.view.state.doc.length,
            insert: e
          }
        });
      } finally {
        this.syncingFromView = !1;
      }
    }
  }
  setDiagnostics(e) {
    if (this.fallback.setDiagnostics(e), !this.view)
      return;
    const t = e.map((i) => om(i, this.view.state)).filter((i) => !!i);
    this.view.dispatch(Fg(this.view.state, t));
  }
  setReadOnly(e) {
    this.fallback.setReadOnly(e), this.view && this.view.dispatch({
      effects: [
        this.readOnly.reconfigure(N.readOnly.of(e)),
        this.editable.reconfigure(T.editable.of(!e))
      ]
    });
  }
  focus() {
    this.view?.focus(), this.view || this.fallback.focus();
  }
  dispose() {
    this.view?.destroy(), this.view = void 0, delete this.textarea.dataset.editor, this.fallback.dispose();
  }
}
function am(n, e) {
  const t = n.findIndex((i) => i.includes(e));
  return t >= 0 ? t + 1 : 1;
}
function hm(n) {
  const e = n.split(/\r?\n/), t = [], i = [];
  n.includes("!! FAIL") && t.push({
    severity: "error",
    message: "Mock-Fehler: Der Code enthält !! FAIL.",
    line: am(e, "!! FAIL"),
    source: "runner"
  });
  for (const [r, o] of e.entries()) {
    const l = o.trim();
    if (!l || l.startsWith("!!"))
      continue;
    const a = l.match(/^MODEL\s+([A-Za-z_][A-Za-z0-9_]*)\b/);
    if (a) {
      i.push({ kind: "MODEL", name: a[1], line: r + 1 });
      continue;
    }
    const h = l.match(/^TOPIC\s+([A-Za-z_][A-Za-z0-9_]*)\s*=/);
    if (h) {
      i.push({ kind: "TOPIC", name: h[1], line: r + 1 });
      continue;
    }
    const c = l.match(/^CLASS\s+([A-Za-z_][A-Za-z0-9_]*)\s*=/);
    if (c) {
      i.push({ kind: "CLASS", name: c[1], line: r + 1 });
      continue;
    }
    const f = l.match(/^END\s+([A-Za-z_][A-Za-z0-9_]*)\s*([.;])/);
    if (f) {
      const u = i.pop(), d = f[1], p = f[2];
      if (!u) {
        t.push({
          severity: "error",
          message: `Unerwartetes END ${d}${p}`,
          line: r + 1,
          source: "runner"
        });
        continue;
      }
      u.name !== d && t.push({
        severity: "error",
        message: `END ${d}${p} passt nicht zu ${u.kind} ${u.name}. Erwartet: END ${u.name}${u.kind === "MODEL" ? "." : ";"}`,
        line: r + 1,
        source: "runner"
      });
    }
  }
  const s = i.find((r) => r.kind === "MODEL");
  s && !new RegExp(`END\\s+${s.name}\\s*\\.`, "m").test(n) && t.push({
    severity: "error",
    message: `Das Modell ${s.name} endet nicht mit "END ${s.name}."`,
    line: s.line,
    source: "runner"
  });
  for (const r of i.reverse())
    t.push({
      severity: "error",
      message: `${r.kind} ${r.name} wurde nicht korrekt geschlossen.`,
      line: r.line,
      source: "runner"
    });
  return t;
}
class cm {
  constructor() {
    this.id = "mock", this.label = "Mock Runner";
  }
  async compile(e) {
    const t = performance.now(), i = hm(e.code), s = i.length === 0;
    return {
      ok: s,
      diagnostics: i,
      stdout: s ? "Mock compile successful" : void 0,
      stderr: s ? void 0 : "Mock compile failed",
      durationMs: Math.round(performance.now() - t)
    };
  }
}
const fm = "https://cjrtnc.leaningtech.com/4.3/loader.js", um = "/ili2c.jar", bc = "/str/model.ili", dm = 12e4, Ds = /* @__PURE__ */ new Map();
let pi, pm = 0;
function Qr() {
  return globalThis;
}
function gm() {
  const n = Qr();
  if (typeof n.cheerpjInit != "function")
    throw new Error("CheerpJ ist nicht geladen.");
  return n;
}
function mm() {
  const n = Qr();
  if (typeof n.cheerpjInit != "function" || typeof n.cheerpjRunJar != "function" || typeof n.cheerpOSAddStringFile != "function" || typeof n.cjFileBlob != "function")
    throw new Error("CheerpJ ist nicht vollständig geladen.");
  return n;
}
function bm(n) {
  if (typeof document > "u")
    return Promise.reject(new Error("Der CheerpJ-Runner benötigt eine Browser-Umgebung."));
  if (typeof Qr().cheerpjInit == "function")
    return Promise.resolve();
  const e = Ds.get(n);
  if (e)
    return e;
  const t = new Promise((i, s) => {
    const r = document.querySelector(
      `script[data-interlis-lab-cheerpj="${n}"]`
    ), o = r ?? document.createElement("script");
    o.addEventListener("load", () => i(), { once: !0 }), o.addEventListener(
      "error",
      () => {
        Ds.delete(n), s(new Error("Der CheerpJ-Loader konnte nicht geladen werden."));
      },
      { once: !0 }
    ), r || (o.src = n, o.async = !0, o.dataset.interlisLabCheerpj = n, document.head.append(o));
  });
  return Ds.set(n, t), t;
}
function ym(n) {
  return pi || (pi = gm().cheerpjInit({
    status: "none",
    version: n.javaVersion ?? 17,
    ...n.cheerpjInitOptions
  }).catch((t) => {
    throw pi = void 0, t;
  }), pi);
}
function wm(n) {
  if (n.startsWith("/app/"))
    return n;
  if (typeof document < "u" && typeof window < "u") {
    const e = new URL(n, document.baseURI);
    if ((e.protocol === "http:" || e.protocol === "https:") && e.origin !== window.location.origin)
      throw new Error("ili2cJarUrl muss für CheerpJ unter demselben Webserver wie die Seite verfügbar sein.");
    return `/app${e.pathname}`;
  }
  return n.startsWith("/") ? `/app${n}` : `/app/${n}`;
}
function vm(n, e) {
  let t;
  const i = new Promise((s, r) => {
    t = setTimeout(() => {
      r(new Error(`ili2c wurde nach ${e} ms abgebrochen.`));
    }, e);
  });
  return Promise.race([n, i]).finally(() => {
    t && clearTimeout(t);
  });
}
function xm(n) {
  switch (n.toLowerCase()) {
    case "warning":
      return "warning";
    case "info":
      return "info";
    default:
      return "error";
  }
}
function km(n, e) {
  return n === e || n.endsWith(`/${e.split("/").pop() ?? ""}`) ? e.split("/").pop() ?? n : n;
}
function Sm(n, e = bc) {
  const t = [];
  for (const i of n.split(/\r?\n/)) {
    const r = i.trim().match(/^(Error|Warning|Info):\s+(.+)$/);
    if (!r)
      continue;
    const o = xm(r[1]);
    let l = r[2];
    if (l.startsWith("...compiler run "))
      continue;
    const a = l.match(/^(.+):(\d+):(.+)$/);
    if (a) {
      l = a[3].trim(), t.push({
        severity: o,
        message: l,
        file: km(a[1], e),
        line: Number(a[2]),
        source: "ili2c"
      });
      continue;
    }
    t.push({
      severity: o,
      message: l,
      source: "ili2c"
    });
  }
  return t.filter((i) => i.severity !== "info");
}
class Am {
  constructor(e = {}) {
    this.options = e, this.id = "cheerpj", this.label = "CheerpJ Runner";
  }
  async init() {
    this.options.executor || (await bm(this.options.cheerpjLoaderUrl ?? fm), await ym(this.options));
  }
  async compile(e) {
    if (this.options.executor)
      return this.options.executor(e, this.options);
    await this.init();
    const t = mm(), i = performance.now(), s = this.options.modelPath ?? bc, r = this.options.logPath ?? `/files/interlis-lab-ili2c-${++pm}.log`, o = wm(this.options.ili2cJarUrl ?? um);
    await t.cheerpOSAddStringFile(s, e.code);
    const l = ["--log", r, ...this.options.javaArgs ?? [], s], a = await vm(
      t.cheerpjRunJar(o, ...l),
      this.options.timeoutMs ?? dm
    ), h = await t.cjFileBlob(r).then((u) => u.text()).catch(() => ""), c = (this.options.parseDiagnostics ?? ((u) => Sm(u, s)))(
      h
    );
    a !== 0 && c.length === 0 && c.push({
      severity: "error",
      message: `ili2c wurde mit Exit-Code ${a} beendet.`,
      source: "ili2c"
    });
    const f = a === 0 && !c.some((u) => u.severity === "error");
    return {
      ok: f,
      diagnostics: c,
      stdout: f ? h : void 0,
      stderr: f ? void 0 : h,
      durationMs: Math.round(performance.now() - i)
    };
  }
}
const Cn = globalThis, eo = Cn.ShadowRoot && (Cn.ShadyCSS === void 0 || Cn.ShadyCSS.nativeShadow) && "adoptedStyleSheets" in Document.prototype && "replace" in CSSStyleSheet.prototype, to = /* @__PURE__ */ Symbol(), Rl = /* @__PURE__ */ new WeakMap();
let yc = class {
  constructor(e, t, i) {
    if (this._$cssResult$ = !0, i !== to) throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");
    this.cssText = e, this.t = t;
  }
  get styleSheet() {
    let e = this.o;
    const t = this.t;
    if (eo && e === void 0) {
      const i = t !== void 0 && t.length === 1;
      i && (e = Rl.get(t)), e === void 0 && ((this.o = e = new CSSStyleSheet()).replaceSync(this.cssText), i && Rl.set(t, e));
    }
    return e;
  }
  toString() {
    return this.cssText;
  }
};
const Cm = (n) => new yc(typeof n == "string" ? n : n + "", void 0, to), Mm = (n, ...e) => {
  const t = n.length === 1 ? n[0] : e.reduce((i, s, r) => i + ((o) => {
    if (o._$cssResult$ === !0) return o.cssText;
    if (typeof o == "number") return o;
    throw Error("Value passed to 'css' function must be a 'css' function result: " + o + ". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.");
  })(s) + n[r + 1], n[0]);
  return new yc(t, n, to);
}, Tm = (n, e) => {
  if (eo) n.adoptedStyleSheets = e.map((t) => t instanceof CSSStyleSheet ? t : t.styleSheet);
  else for (const t of e) {
    const i = document.createElement("style"), s = Cn.litNonce;
    s !== void 0 && i.setAttribute("nonce", s), i.textContent = t.cssText, n.appendChild(i);
  }
}, Bl = eo ? (n) => n : (n) => n instanceof CSSStyleSheet ? ((e) => {
  let t = "";
  for (const i of e.cssRules) t += i.cssText;
  return Cm(t);
})(n) : n;
const { is: Om, defineProperty: Dm, getOwnPropertyDescriptor: Em, getOwnPropertyNames: Lm, getOwnPropertySymbols: Rm, getPrototypeOf: Bm } = Object, rs = globalThis, Pl = rs.trustedTypes, Pm = Pl ? Pl.emptyScript : "", Im = rs.reactiveElementPolyfillSupport, Mi = (n, e) => n, Wn = { toAttribute(n, e) {
  switch (e) {
    case Boolean:
      n = n ? Pm : null;
      break;
    case Object:
    case Array:
      n = n == null ? n : JSON.stringify(n);
  }
  return n;
}, fromAttribute(n, e) {
  let t = n;
  switch (e) {
    case Boolean:
      t = n !== null;
      break;
    case Number:
      t = n === null ? null : Number(n);
      break;
    case Object:
    case Array:
      try {
        t = JSON.parse(n);
      } catch {
        t = null;
      }
  }
  return t;
} }, io = (n, e) => !Om(n, e), Il = { attribute: !0, type: String, converter: Wn, reflect: !1, useDefault: !1, hasChanged: io };
Symbol.metadata ??= /* @__PURE__ */ Symbol("metadata"), rs.litPropertyMetadata ??= /* @__PURE__ */ new WeakMap();
let Ft = class extends HTMLElement {
  static addInitializer(e) {
    this._$Ei(), (this.l ??= []).push(e);
  }
  static get observedAttributes() {
    return this.finalize(), this._$Eh && [...this._$Eh.keys()];
  }
  static createProperty(e, t = Il) {
    if (t.state && (t.attribute = !1), this._$Ei(), this.prototype.hasOwnProperty(e) && ((t = Object.create(t)).wrapped = !0), this.elementProperties.set(e, t), !t.noAccessor) {
      const i = /* @__PURE__ */ Symbol(), s = this.getPropertyDescriptor(e, i, t);
      s !== void 0 && Dm(this.prototype, e, s);
    }
  }
  static getPropertyDescriptor(e, t, i) {
    const { get: s, set: r } = Em(this.prototype, e) ?? { get() {
      return this[t];
    }, set(o) {
      this[t] = o;
    } };
    return { get: s, set(o) {
      const l = s?.call(this);
      r?.call(this, o), this.requestUpdate(e, l, i);
    }, configurable: !0, enumerable: !0 };
  }
  static getPropertyOptions(e) {
    return this.elementProperties.get(e) ?? Il;
  }
  static _$Ei() {
    if (this.hasOwnProperty(Mi("elementProperties"))) return;
    const e = Bm(this);
    e.finalize(), e.l !== void 0 && (this.l = [...e.l]), this.elementProperties = new Map(e.elementProperties);
  }
  static finalize() {
    if (this.hasOwnProperty(Mi("finalized"))) return;
    if (this.finalized = !0, this._$Ei(), this.hasOwnProperty(Mi("properties"))) {
      const t = this.properties, i = [...Lm(t), ...Rm(t)];
      for (const s of i) this.createProperty(s, t[s]);
    }
    const e = this[Symbol.metadata];
    if (e !== null) {
      const t = litPropertyMetadata.get(e);
      if (t !== void 0) for (const [i, s] of t) this.elementProperties.set(i, s);
    }
    this._$Eh = /* @__PURE__ */ new Map();
    for (const [t, i] of this.elementProperties) {
      const s = this._$Eu(t, i);
      s !== void 0 && this._$Eh.set(s, t);
    }
    this.elementStyles = this.finalizeStyles(this.styles);
  }
  static finalizeStyles(e) {
    const t = [];
    if (Array.isArray(e)) {
      const i = new Set(e.flat(1 / 0).reverse());
      for (const s of i) t.unshift(Bl(s));
    } else e !== void 0 && t.push(Bl(e));
    return t;
  }
  static _$Eu(e, t) {
    const i = t.attribute;
    return i === !1 ? void 0 : typeof i == "string" ? i : typeof e == "string" ? e.toLowerCase() : void 0;
  }
  constructor() {
    super(), this._$Ep = void 0, this.isUpdatePending = !1, this.hasUpdated = !1, this._$Em = null, this._$Ev();
  }
  _$Ev() {
    this._$ES = new Promise((e) => this.enableUpdating = e), this._$AL = /* @__PURE__ */ new Map(), this._$E_(), this.requestUpdate(), this.constructor.l?.forEach((e) => e(this));
  }
  addController(e) {
    (this._$EO ??= /* @__PURE__ */ new Set()).add(e), this.renderRoot !== void 0 && this.isConnected && e.hostConnected?.();
  }
  removeController(e) {
    this._$EO?.delete(e);
  }
  _$E_() {
    const e = /* @__PURE__ */ new Map(), t = this.constructor.elementProperties;
    for (const i of t.keys()) this.hasOwnProperty(i) && (e.set(i, this[i]), delete this[i]);
    e.size > 0 && (this._$Ep = e);
  }
  createRenderRoot() {
    const e = this.shadowRoot ?? this.attachShadow(this.constructor.shadowRootOptions);
    return Tm(e, this.constructor.elementStyles), e;
  }
  connectedCallback() {
    this.renderRoot ??= this.createRenderRoot(), this.enableUpdating(!0), this._$EO?.forEach((e) => e.hostConnected?.());
  }
  enableUpdating(e) {
  }
  disconnectedCallback() {
    this._$EO?.forEach((e) => e.hostDisconnected?.());
  }
  attributeChangedCallback(e, t, i) {
    this._$AK(e, i);
  }
  _$ET(e, t) {
    const i = this.constructor.elementProperties.get(e), s = this.constructor._$Eu(e, i);
    if (s !== void 0 && i.reflect === !0) {
      const r = (i.converter?.toAttribute !== void 0 ? i.converter : Wn).toAttribute(t, i.type);
      this._$Em = e, r == null ? this.removeAttribute(s) : this.setAttribute(s, r), this._$Em = null;
    }
  }
  _$AK(e, t) {
    const i = this.constructor, s = i._$Eh.get(e);
    if (s !== void 0 && this._$Em !== s) {
      const r = i.getPropertyOptions(s), o = typeof r.converter == "function" ? { fromAttribute: r.converter } : r.converter?.fromAttribute !== void 0 ? r.converter : Wn;
      this._$Em = s;
      const l = o.fromAttribute(t, r.type);
      this[s] = l ?? this._$Ej?.get(s) ?? l, this._$Em = null;
    }
  }
  requestUpdate(e, t, i, s = !1, r) {
    if (e !== void 0) {
      const o = this.constructor;
      if (s === !1 && (r = this[e]), i ??= o.getPropertyOptions(e), !((i.hasChanged ?? io)(r, t) || i.useDefault && i.reflect && r === this._$Ej?.get(e) && !this.hasAttribute(o._$Eu(e, i)))) return;
      this.C(e, t, i);
    }
    this.isUpdatePending === !1 && (this._$ES = this._$EP());
  }
  C(e, t, { useDefault: i, reflect: s, wrapped: r }, o) {
    i && !(this._$Ej ??= /* @__PURE__ */ new Map()).has(e) && (this._$Ej.set(e, o ?? t ?? this[e]), r !== !0 || o !== void 0) || (this._$AL.has(e) || (this.hasUpdated || i || (t = void 0), this._$AL.set(e, t)), s === !0 && this._$Em !== e && (this._$Eq ??= /* @__PURE__ */ new Set()).add(e));
  }
  async _$EP() {
    this.isUpdatePending = !0;
    try {
      await this._$ES;
    } catch (t) {
      Promise.reject(t);
    }
    const e = this.scheduleUpdate();
    return e != null && await e, !this.isUpdatePending;
  }
  scheduleUpdate() {
    return this.performUpdate();
  }
  performUpdate() {
    if (!this.isUpdatePending) return;
    if (!this.hasUpdated) {
      if (this.renderRoot ??= this.createRenderRoot(), this._$Ep) {
        for (const [s, r] of this._$Ep) this[s] = r;
        this._$Ep = void 0;
      }
      const i = this.constructor.elementProperties;
      if (i.size > 0) for (const [s, r] of i) {
        const { wrapped: o } = r, l = this[s];
        o !== !0 || this._$AL.has(s) || l === void 0 || this.C(s, void 0, r, l);
      }
    }
    let e = !1;
    const t = this._$AL;
    try {
      e = this.shouldUpdate(t), e ? (this.willUpdate(t), this._$EO?.forEach((i) => i.hostUpdate?.()), this.update(t)) : this._$EM();
    } catch (i) {
      throw e = !1, this._$EM(), i;
    }
    e && this._$AE(t);
  }
  willUpdate(e) {
  }
  _$AE(e) {
    this._$EO?.forEach((t) => t.hostUpdated?.()), this.hasUpdated || (this.hasUpdated = !0, this.firstUpdated(e)), this.updated(e);
  }
  _$EM() {
    this._$AL = /* @__PURE__ */ new Map(), this.isUpdatePending = !1;
  }
  get updateComplete() {
    return this.getUpdateComplete();
  }
  getUpdateComplete() {
    return this._$ES;
  }
  shouldUpdate(e) {
    return !0;
  }
  update(e) {
    this._$Eq &&= this._$Eq.forEach((t) => this._$ET(t, this[t])), this._$EM();
  }
  updated(e) {
  }
  firstUpdated(e) {
  }
};
Ft.elementStyles = [], Ft.shadowRootOptions = { mode: "open" }, Ft[Mi("elementProperties")] = /* @__PURE__ */ new Map(), Ft[Mi("finalized")] = /* @__PURE__ */ new Map(), Im?.({ ReactiveElement: Ft }), (rs.reactiveElementVersions ??= []).push("2.1.2");
const no = globalThis, $l = (n) => n, _n = no.trustedTypes, Nl = _n ? _n.createPolicy("lit-html", { createHTML: (n) => n }) : void 0, wc = "$lit$", ct = `lit$${Math.random().toFixed(9).slice(2)}$`, vc = "?" + ct, $m = `<${vc}>`, Nt = document, Vi = () => Nt.createComment(""), Fi = (n) => n === null || typeof n != "object" && typeof n != "function", so = Array.isArray, Nm = (n) => so(n) || typeof n?.[Symbol.iterator] == "function", Es = `[ 	
\f\r]`, gi = /<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g, Hl = /-->/g, Vl = />/g, kt = RegExp(`>|${Es}(?:([^\\s"'>=/]+)(${Es}*=${Es}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`, "g"), Fl = /'/g, Wl = /"/g, xc = /^(?:script|style|textarea|title)$/i, Hm = (n) => (e, ...t) => ({ _$litType$: n, strings: e, values: t }), Q = Hm(1), ri = /* @__PURE__ */ Symbol.for("lit-noChange"), V = /* @__PURE__ */ Symbol.for("lit-nothing"), _l = /* @__PURE__ */ new WeakMap(), Ot = Nt.createTreeWalker(Nt, 129);
function kc(n, e) {
  if (!so(n) || !n.hasOwnProperty("raw")) throw Error("invalid template strings array");
  return Nl !== void 0 ? Nl.createHTML(e) : e;
}
const Vm = (n, e) => {
  const t = n.length - 1, i = [];
  let s, r = e === 2 ? "<svg>" : e === 3 ? "<math>" : "", o = gi;
  for (let l = 0; l < t; l++) {
    const a = n[l];
    let h, c, f = -1, u = 0;
    for (; u < a.length && (o.lastIndex = u, c = o.exec(a), c !== null); ) u = o.lastIndex, o === gi ? c[1] === "!--" ? o = Hl : c[1] !== void 0 ? o = Vl : c[2] !== void 0 ? (xc.test(c[2]) && (s = RegExp("</" + c[2], "g")), o = kt) : c[3] !== void 0 && (o = kt) : o === kt ? c[0] === ">" ? (o = s ?? gi, f = -1) : c[1] === void 0 ? f = -2 : (f = o.lastIndex - c[2].length, h = c[1], o = c[3] === void 0 ? kt : c[3] === '"' ? Wl : Fl) : o === Wl || o === Fl ? o = kt : o === Hl || o === Vl ? o = gi : (o = kt, s = void 0);
    const d = o === kt && n[l + 1].startsWith("/>") ? " " : "";
    r += o === gi ? a + $m : f >= 0 ? (i.push(h), a.slice(0, f) + wc + a.slice(f) + ct + d) : a + ct + (f === -2 ? l : d);
  }
  return [kc(n, r + (n[t] || "<?>") + (e === 2 ? "</svg>" : e === 3 ? "</math>" : "")), i];
};
class Wi {
  constructor({ strings: e, _$litType$: t }, i) {
    let s;
    this.parts = [];
    let r = 0, o = 0;
    const l = e.length - 1, a = this.parts, [h, c] = Vm(e, t);
    if (this.el = Wi.createElement(h, i), Ot.currentNode = this.el.content, t === 2 || t === 3) {
      const f = this.el.content.firstChild;
      f.replaceWith(...f.childNodes);
    }
    for (; (s = Ot.nextNode()) !== null && a.length < l; ) {
      if (s.nodeType === 1) {
        if (s.hasAttributes()) for (const f of s.getAttributeNames()) if (f.endsWith(wc)) {
          const u = c[o++], d = s.getAttribute(f).split(ct), p = /([.?@])?(.*)/.exec(u);
          a.push({ type: 1, index: r, name: p[2], strings: d, ctor: p[1] === "." ? Wm : p[1] === "?" ? _m : p[1] === "@" ? zm : os }), s.removeAttribute(f);
        } else f.startsWith(ct) && (a.push({ type: 6, index: r }), s.removeAttribute(f));
        if (xc.test(s.tagName)) {
          const f = s.textContent.split(ct), u = f.length - 1;
          if (u > 0) {
            s.textContent = _n ? _n.emptyScript : "";
            for (let d = 0; d < u; d++) s.append(f[d], Vi()), Ot.nextNode(), a.push({ type: 2, index: ++r });
            s.append(f[u], Vi());
          }
        }
      } else if (s.nodeType === 8) if (s.data === vc) a.push({ type: 2, index: r });
      else {
        let f = -1;
        for (; (f = s.data.indexOf(ct, f + 1)) !== -1; ) a.push({ type: 7, index: r }), f += ct.length - 1;
      }
      r++;
    }
  }
  static createElement(e, t) {
    const i = Nt.createElement("template");
    return i.innerHTML = e, i;
  }
}
function oi(n, e, t = n, i) {
  if (e === ri) return e;
  let s = i !== void 0 ? t._$Co?.[i] : t._$Cl;
  const r = Fi(e) ? void 0 : e._$litDirective$;
  return s?.constructor !== r && (s?._$AO?.(!1), r === void 0 ? s = void 0 : (s = new r(n), s._$AT(n, t, i)), i !== void 0 ? (t._$Co ??= [])[i] = s : t._$Cl = s), s !== void 0 && (e = oi(n, s._$AS(n, e.values), s, i)), e;
}
class Fm {
  constructor(e, t) {
    this._$AV = [], this._$AN = void 0, this._$AD = e, this._$AM = t;
  }
  get parentNode() {
    return this._$AM.parentNode;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  u(e) {
    const { el: { content: t }, parts: i } = this._$AD, s = (e?.creationScope ?? Nt).importNode(t, !0);
    Ot.currentNode = s;
    let r = Ot.nextNode(), o = 0, l = 0, a = i[0];
    for (; a !== void 0; ) {
      if (o === a.index) {
        let h;
        a.type === 2 ? h = new Gi(r, r.nextSibling, this, e) : a.type === 1 ? h = new a.ctor(r, a.name, a.strings, this, e) : a.type === 6 && (h = new Um(r, this, e)), this._$AV.push(h), a = i[++l];
      }
      o !== a?.index && (r = Ot.nextNode(), o++);
    }
    return Ot.currentNode = Nt, s;
  }
  p(e) {
    let t = 0;
    for (const i of this._$AV) i !== void 0 && (i.strings !== void 0 ? (i._$AI(e, i, t), t += i.strings.length - 2) : i._$AI(e[t])), t++;
  }
}
class Gi {
  get _$AU() {
    return this._$AM?._$AU ?? this._$Cv;
  }
  constructor(e, t, i, s) {
    this.type = 2, this._$AH = V, this._$AN = void 0, this._$AA = e, this._$AB = t, this._$AM = i, this.options = s, this._$Cv = s?.isConnected ?? !0;
  }
  get parentNode() {
    let e = this._$AA.parentNode;
    const t = this._$AM;
    return t !== void 0 && e?.nodeType === 11 && (e = t.parentNode), e;
  }
  get startNode() {
    return this._$AA;
  }
  get endNode() {
    return this._$AB;
  }
  _$AI(e, t = this) {
    e = oi(this, e, t), Fi(e) ? e === V || e == null || e === "" ? (this._$AH !== V && this._$AR(), this._$AH = V) : e !== this._$AH && e !== ri && this._(e) : e._$litType$ !== void 0 ? this.$(e) : e.nodeType !== void 0 ? this.T(e) : Nm(e) ? this.k(e) : this._(e);
  }
  O(e) {
    return this._$AA.parentNode.insertBefore(e, this._$AB);
  }
  T(e) {
    this._$AH !== e && (this._$AR(), this._$AH = this.O(e));
  }
  _(e) {
    this._$AH !== V && Fi(this._$AH) ? this._$AA.nextSibling.data = e : this.T(Nt.createTextNode(e)), this._$AH = e;
  }
  $(e) {
    const { values: t, _$litType$: i } = e, s = typeof i == "number" ? this._$AC(e) : (i.el === void 0 && (i.el = Wi.createElement(kc(i.h, i.h[0]), this.options)), i);
    if (this._$AH?._$AD === s) this._$AH.p(t);
    else {
      const r = new Fm(s, this), o = r.u(this.options);
      r.p(t), this.T(o), this._$AH = r;
    }
  }
  _$AC(e) {
    let t = _l.get(e.strings);
    return t === void 0 && _l.set(e.strings, t = new Wi(e)), t;
  }
  k(e) {
    so(this._$AH) || (this._$AH = [], this._$AR());
    const t = this._$AH;
    let i, s = 0;
    for (const r of e) s === t.length ? t.push(i = new Gi(this.O(Vi()), this.O(Vi()), this, this.options)) : i = t[s], i._$AI(r), s++;
    s < t.length && (this._$AR(i && i._$AB.nextSibling, s), t.length = s);
  }
  _$AR(e = this._$AA.nextSibling, t) {
    for (this._$AP?.(!1, !0, t); e !== this._$AB; ) {
      const i = $l(e).nextSibling;
      $l(e).remove(), e = i;
    }
  }
  setConnected(e) {
    this._$AM === void 0 && (this._$Cv = e, this._$AP?.(e));
  }
}
class os {
  get tagName() {
    return this.element.tagName;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  constructor(e, t, i, s, r) {
    this.type = 1, this._$AH = V, this._$AN = void 0, this.element = e, this.name = t, this._$AM = s, this.options = r, i.length > 2 || i[0] !== "" || i[1] !== "" ? (this._$AH = Array(i.length - 1).fill(new String()), this.strings = i) : this._$AH = V;
  }
  _$AI(e, t = this, i, s) {
    const r = this.strings;
    let o = !1;
    if (r === void 0) e = oi(this, e, t, 0), o = !Fi(e) || e !== this._$AH && e !== ri, o && (this._$AH = e);
    else {
      const l = e;
      let a, h;
      for (e = r[0], a = 0; a < r.length - 1; a++) h = oi(this, l[i + a], t, a), h === ri && (h = this._$AH[a]), o ||= !Fi(h) || h !== this._$AH[a], h === V ? e = V : e !== V && (e += (h ?? "") + r[a + 1]), this._$AH[a] = h;
    }
    o && !s && this.j(e);
  }
  j(e) {
    e === V ? this.element.removeAttribute(this.name) : this.element.setAttribute(this.name, e ?? "");
  }
}
class Wm extends os {
  constructor() {
    super(...arguments), this.type = 3;
  }
  j(e) {
    this.element[this.name] = e === V ? void 0 : e;
  }
}
class _m extends os {
  constructor() {
    super(...arguments), this.type = 4;
  }
  j(e) {
    this.element.toggleAttribute(this.name, !!e && e !== V);
  }
}
class zm extends os {
  constructor(e, t, i, s, r) {
    super(e, t, i, s, r), this.type = 5;
  }
  _$AI(e, t = this) {
    if ((e = oi(this, e, t, 0) ?? V) === ri) return;
    const i = this._$AH, s = e === V && i !== V || e.capture !== i.capture || e.once !== i.once || e.passive !== i.passive, r = e !== V && (i === V || s);
    s && this.element.removeEventListener(this.name, this, i), r && this.element.addEventListener(this.name, this, e), this._$AH = e;
  }
  handleEvent(e) {
    typeof this._$AH == "function" ? this._$AH.call(this.options?.host ?? this.element, e) : this._$AH.handleEvent(e);
  }
}
class Um {
  constructor(e, t, i) {
    this.element = e, this.type = 6, this._$AN = void 0, this._$AM = t, this.options = i;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(e) {
    oi(this, e);
  }
}
const jm = no.litHtmlPolyfillSupport;
jm?.(Wi, Gi), (no.litHtmlVersions ??= []).push("3.3.2");
const qm = (n, e, t) => {
  const i = t?.renderBefore ?? e;
  let s = i._$litPart$;
  if (s === void 0) {
    const r = t?.renderBefore ?? null;
    i._$litPart$ = s = new Gi(e.insertBefore(Vi(), r), r, void 0, t ?? {});
  }
  return s._$AI(n), s;
};
const ro = globalThis;
class Ti extends Ft {
  constructor() {
    super(...arguments), this.renderOptions = { host: this }, this._$Do = void 0;
  }
  createRenderRoot() {
    const e = super.createRenderRoot();
    return this.renderOptions.renderBefore ??= e.firstChild, e;
  }
  update(e) {
    const t = this.render();
    this.hasUpdated || (this.renderOptions.isConnected = this.isConnected), super.update(e), this._$Do = qm(t, this.renderRoot, this.renderOptions);
  }
  connectedCallback() {
    super.connectedCallback(), this._$Do?.setConnected(!0);
  }
  disconnectedCallback() {
    super.disconnectedCallback(), this._$Do?.setConnected(!1);
  }
  render() {
    return ri;
  }
}
Ti._$litElement$ = !0, Ti.finalized = !0, ro.litElementHydrateSupport?.({ LitElement: Ti });
const Km = ro.litElementPolyfillSupport;
Km?.({ LitElement: Ti });
(ro.litElementVersions ??= []).push("4.2.2");
const Gm = { attribute: !0, type: String, converter: Wn, reflect: !1, hasChanged: io }, Jm = (n = Gm, e, t) => {
  const { kind: i, metadata: s } = t;
  let r = globalThis.litPropertyMetadata.get(s);
  if (r === void 0 && globalThis.litPropertyMetadata.set(s, r = /* @__PURE__ */ new Map()), i === "setter" && ((n = Object.create(n)).wrapped = !0), r.set(t.name, n), i === "accessor") {
    const { name: o } = t;
    return { set(l) {
      const a = e.get.call(this);
      e.set.call(this, l), this.requestUpdate(o, a, n, !0, l);
    }, init(l) {
      return l !== void 0 && this.C(o, void 0, n, l), l;
    } };
  }
  if (i === "setter") {
    const { name: o } = t;
    return function(l) {
      const a = this[o];
      e.call(this, l), this.requestUpdate(o, a, n, !0, l);
    };
  }
  throw Error("Unsupported decorator location: " + i);
};
function Ve(n) {
  return (e, t) => typeof t == "object" ? Jm(n, e, t) : ((i, s, r) => {
    const o = s.hasOwnProperty(r);
    return s.constructor.createProperty(r, i), o ? Object.getOwnPropertyDescriptor(s, r) : void 0;
  })(n, e, t);
}
function oo(n) {
  return Ve({ ...n, state: !0, attribute: !1 });
}
const Ym = (n, e, t) => (t.configurable = !0, t.enumerable = !0, Reflect.decorate && typeof e != "object" && Object.defineProperty(n, e, t), t);
function Xm(n, e) {
  return (t, i, s) => {
    const r = (o) => o.renderRoot?.querySelector(n) ?? null;
    return Ym(t, i, { get() {
      return r(this);
    } });
  };
}
const Zm = Mm`
  :host {
    --interlis-lab-border-color: #d9e1ea;
    --interlis-lab-border-strong: #c2cedb;
    --interlis-lab-background: #ffffff;
    --interlis-lab-panel-background: #f7f9fc;
    --interlis-lab-panel-muted: #eef3f7;
    --interlis-lab-text-color: #172033;
    --interlis-lab-muted-color: #647184;
    --interlis-lab-success-color: #15803d;
    --interlis-lab-error-color: #b42318;
    --interlis-lab-warning-color: #a15c07;
    --interlis-lab-accent-color: #0f766e;
    --interlis-lab-radius: 0.5rem;
    --interlis-lab-font-family: system-ui, sans-serif;
    --interlis-lab-monospace-font: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    color: var(--interlis-lab-text-color);
    display: block;
    font-family: var(--interlis-lab-font-family);
  }

  *,
  *::before,
  *::after {
    box-sizing: border-box;
  }

  h2,
  h3,
  h4,
  p {
    margin: 0;
  }

  .shell {
    background: var(--interlis-lab-background);
    border: 1px solid var(--interlis-lab-border-color);
    border-radius: var(--interlis-lab-radius);
    overflow: hidden;
  }

  .section,
  .support-section {
    border-top: 1px solid var(--interlis-lab-border-color);
    padding: 1rem 1.25rem;
  }

  .section:first-child {
    border-top: none;
  }

  .lab-header {
    align-items: start;
    border-bottom: 1px solid var(--interlis-lab-border-color);
    display: flex;
    gap: 1rem;
    justify-content: space-between;
    padding: 1.15rem 1.25rem 1rem;
  }

  .title-stack {
    display: grid;
    gap: 0.35rem;
    min-width: 0;
  }

  .eyebrow,
  .panel-label {
    color: var(--interlis-lab-muted-color);
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0;
    text-transform: uppercase;
  }

  h2 {
    font-size: 1.28rem;
    line-height: 1.25;
  }

  .meta-row {
    color: var(--interlis-lab-muted-color);
    display: flex;
    flex-wrap: wrap;
    font-size: 0.82rem;
    gap: 0.4rem 0.7rem;
  }

  .meta-row span:not(:first-child)::before {
    color: #9aa7b6;
    content: '/';
    margin-right: 0.7rem;
  }

  .status-line {
    align-items: center;
    color: var(--interlis-lab-muted-color);
    display: inline-flex;
    flex: 0 0 auto;
    font-size: 0.86rem;
    font-weight: 650;
    gap: 0.45rem;
    padding-top: 0.15rem;
    white-space: nowrap;
  }

  .status-dot {
    background: currentColor;
    border-radius: 50%;
    height: 0.55rem;
    width: 0.55rem;
  }

  .status-line[data-status='running'] {
    color: var(--interlis-lab-accent-color);
  }

  .status-line[data-status='success'] {
    color: var(--interlis-lab-success-color);
  }

  .status-line[data-status='failed'],
  .status-line[data-status='error'] {
    color: var(--interlis-lab-error-color);
  }

  .workbench {
    display: grid;
    gap: 0;
  }

  .lesson-panel {
    background: var(--interlis-lab-panel-background);
    border-bottom: 1px solid var(--interlis-lab-border-color);
    display: flex;
    flex-direction: column;
    gap: 1rem;
    padding: 1.1rem 1.25rem;
  }

  .lesson-panel h3 {
    font-size: 1.02rem;
    line-height: 1.45;
  }

  .lesson-context {
    display: grid;
    gap: 0.85rem;
  }

  .lesson-context section {
    display: grid;
    gap: 0.25rem;
  }

  .lesson-context h4 {
    color: var(--interlis-lab-muted-color);
    font-size: 0.8rem;
    font-weight: 700;
  }

  .lesson-context p,
  .feedback-summary p,
  .hint-item p,
  .reflection-panel p {
    color: var(--interlis-lab-muted-color);
    line-height: 1.55;
  }

  .workspace {
    display: grid;
    gap: 0.85rem;
    min-width: 0;
    padding: 1rem;
  }

  .editor-frame {
    background: #fbfcfd;
    border: 1px solid var(--interlis-lab-border-strong);
    border-radius: var(--interlis-lab-radius);
    overflow: hidden;
  }

  .editor-header {
    align-items: center;
    background: #f6f8fb;
    border-bottom: 1px solid var(--interlis-lab-border-color);
    color: var(--interlis-lab-muted-color);
    display: flex;
    font-size: 0.78rem;
    gap: 0.5rem;
    justify-content: space-between;
    padding: 0.55rem 0.75rem;
  }

  .editor-title {
    color: var(--interlis-lab-text-color);
    font-weight: 700;
  }

  .editor-host {
    min-height: 22rem;
    position: relative;
  }

  .native-editor {
    background: #fbfcfd;
    border: none;
    color: var(--interlis-lab-text-color);
    display: block;
    font-family: var(--interlis-lab-monospace-font);
    font-size: 0.9rem;
    line-height: 1.55;
    min-height: 22rem;
    outline: none;
    padding: 1rem;
    resize: vertical;
    width: 100%;
  }

  .native-editor[data-editor='codemirror'] {
    height: 1px;
    left: 0;
    min-height: 0;
    opacity: 0;
    overflow: hidden;
    padding: 0;
    pointer-events: none;
    position: absolute;
    top: 0;
    width: 1px;
  }

  .native-editor::placeholder {
    color: #8a98aa;
  }

  .native-editor[readonly] {
    opacity: 0.82;
  }

  .cm-editor {
    min-height: 22rem;
  }

  .toolbar {
    align-items: center;
    display: flex;
    flex-wrap: wrap;
    gap: 0.55rem;
  }

  button {
    appearance: none;
    background: #ffffff;
    border: 1px solid var(--interlis-lab-border-strong);
    border-radius: 0.45rem;
    color: var(--interlis-lab-text-color);
    cursor: pointer;
    font: inherit;
    font-size: 0.9rem;
    font-weight: 650;
    min-height: 2.35rem;
    padding: 0.55rem 0.75rem;
    transition:
      background-color 140ms ease,
      border-color 140ms ease,
      color 140ms ease;
  }

  button:hover:not(:disabled) {
    background: #f6fbfa;
    border-color: var(--interlis-lab-accent-color);
    color: var(--interlis-lab-accent-color);
  }

  button:focus-visible {
    outline: 2px solid rgba(15, 118, 110, 0.45);
    outline-offset: 2px;
  }

  button:disabled {
    cursor: not-allowed;
    opacity: 0.48;
  }

  .primary {
    background: var(--interlis-lab-accent-color);
    border-color: var(--interlis-lab-accent-color);
    color: #ffffff;
  }

  .primary:hover:not(:disabled) {
    background: #0b665f;
    color: #ffffff;
  }

  .feedback {
    display: grid;
    gap: 0.7rem;
  }

  .feedback-summary {
    background: #ffffff;
    border: 1px solid var(--interlis-lab-border-color);
    border-left: 3px solid var(--interlis-lab-border-strong);
    border-radius: var(--interlis-lab-radius);
    display: grid;
    gap: 0.2rem;
    padding: 0.75rem 0.85rem;
  }

  .feedback-summary strong {
    font-size: 0.9rem;
  }

  .feedback-summary[data-tone='success'] {
    border-left-color: var(--interlis-lab-success-color);
  }

  .feedback-summary[data-tone='failed'],
  .feedback-summary[data-tone='error'] {
    border-left-color: var(--interlis-lab-error-color);
  }

  .feedback-summary[data-tone='running'] {
    border-left-color: var(--interlis-lab-accent-color);
  }

  .feedback-summary[data-tone='idle'],
  .feedback-summary[data-tone='ready'],
  .feedback-summary[data-tone='loading'] {
    border-left-color: var(--interlis-lab-warning-color);
  }

  .diagnostics {
    display: grid;
    gap: 0.55rem;
    list-style: none;
    margin: 0;
    padding: 0;
  }

  .diagnostics li {
    background: #ffffff;
    border: 1px solid var(--interlis-lab-border-color);
    border-left: 3px solid var(--interlis-lab-border-strong);
    border-radius: var(--interlis-lab-radius);
    padding: 0.65rem 0.75rem;
  }

  .diagnostic-meta {
    color: var(--interlis-lab-muted-color);
    display: block;
    font-size: 0.76rem;
    font-weight: 700;
    margin-bottom: 0.25rem;
  }

  .diagnostic-error {
    border-left-color: var(--interlis-lab-error-color);
  }

  .diagnostic-warning {
    border-left-color: var(--interlis-lab-warning-color);
  }

  .diagnostic-info {
    border-left-color: var(--interlis-lab-accent-color);
  }

  details {
    background: #ffffff;
    border: 1px solid var(--interlis-lab-border-color);
    border-radius: var(--interlis-lab-radius);
    padding: 0.65rem 0.75rem;
  }

  summary {
    color: var(--interlis-lab-text-color);
    cursor: pointer;
    font-size: 0.9rem;
    font-weight: 650;
  }

  pre {
    background: #111827;
    border-radius: 0.4rem;
    color: #dbe7ff;
    font-family: var(--interlis-lab-monospace-font);
    font-size: 0.85rem;
    margin: 0.65rem 0 0;
    overflow: auto;
    padding: 0.75rem;
    white-space: pre-wrap;
  }

  .hint-list,
  .solution-grid {
    display: grid;
    gap: 0.75rem;
  }

  .hint-item,
  .solution-panel,
  .reflection-panel {
    background: var(--interlis-lab-panel-background);
    border: 1px solid var(--interlis-lab-border-color);
    border-radius: var(--interlis-lab-radius);
    padding: 0.85rem 0.95rem;
  }

  .hint-item h4,
  .solution-panel h4,
  .reflection-panel h4 {
    font-size: 0.92rem;
    margin-bottom: 0.35rem;
  }

  .diff-table {
    border-collapse: collapse;
    font-family: var(--interlis-lab-monospace-font);
    font-size: 0.82rem;
    margin-top: 0.65rem;
    width: 100%;
  }

  .diff-table th,
  .diff-table td {
    border: 1px solid var(--interlis-lab-border-color);
    padding: 0.42rem 0.5rem;
    text-align: left;
    vertical-align: top;
  }

  .diff-table th {
    background: var(--interlis-lab-panel-muted);
    color: var(--interlis-lab-muted-color);
    font-weight: 700;
  }

  .diff-added td {
    background: rgba(21, 128, 61, 0.08);
  }

  .diff-removed td {
    background: rgba(180, 35, 24, 0.08);
  }

  .diff-changed td {
    background: rgba(161, 92, 7, 0.09);
  }

  .empty-state {
    color: var(--interlis-lab-muted-color);
    line-height: 1.6;
  }

  @media (min-width: 900px) {
    .workbench {
      grid-template-columns: minmax(16rem, 0.8fr) minmax(0, 1.6fr);
    }

    .lesson-panel {
      border-bottom: none;
      border-right: 1px solid var(--interlis-lab-border-color);
    }

    .solution-grid {
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    }
  }

  @media (max-width: 720px) {
    .lab-header {
      align-items: stretch;
      flex-direction: column;
      padding: 1rem;
    }

    .status-line {
      align-self: flex-start;
    }

    .lesson-panel,
    .workspace,
    .support-section,
    .section {
      padding: 0.9rem;
    }

    .editor-host,
    .native-editor,
    .cm-editor,
    .cm-scroller {
      min-height: 17rem;
    }
  }
`;
var Qm = Object.defineProperty, ve = (n, e, t, i) => {
  for (var s = void 0, r = n.length - 1, o; r >= 0; r--)
    (o = n[r]) && (s = o(e, t, s) || s);
  return s && Qm(e, t, s), s;
};
const e0 = {
  status: "idle",
  code: "",
  visibleHintCount: 0,
  solutionVisible: !1,
  dirty: !1
}, lo = class lo extends Ti {
  constructor() {
    super(...arguments), this.readonly = !1, this.showSolution = !1, this.runner = "mock", this.theme = "auto", this.labState = e0, this.initializedRunners = /* @__PURE__ */ new WeakSet(), this.lessonRequestToken = 0, this.handleEditorInput = (e) => {
      if (!this.activeLesson)
        return;
      const t = e.currentTarget;
      this.labState = {
        ...this.labState,
        code: t.value,
        dirty: t.value !== this.activeLesson.initialCode
      }, this.persistCode(t.value);
    }, this.handleHint = () => {
      const e = this.activeLesson, t = e?.hints?.length ?? 0;
      !e || t === 0 || this.labState.visibleHintCount >= t || (this.labState = Nc(this.labState, t), this.dispatchEvent(
        ot(rt.hintShown, {
          lessonId: e.id,
          visibleHintCount: this.labState.visibleHintCount
        })
      ));
    }, this.handleSolution = () => {
      const e = this.activeLesson;
      !e?.solution || this.labState.solutionVisible || (this.labState = Hc(this.labState), this.dispatchEvent(
        ot(rt.solutionShown, {
          lessonId: e.id
        })
      ));
    }, this.handleReset = () => {
      const e = this.activeLesson;
      e && (this.clearStoredCode(), this.labState = co(e, {
        code: e.initialCode,
        showSolution: this.showSolution
      }), this.dispatchEvent(
        ot(rt.reset, {
          lessonId: e.id
        })
      ), this.updateComplete.then(() => this.editorAdapter?.focus()));
    }, this.handleRun = async () => {
      const e = this.activeLesson;
      if (e)
        try {
          const t = await this.getRunner();
          t.init && !this.initializedRunners.has(t) && (await t.init(), this.initializedRunners.add(t)), this.labState = Ic(this.labState), this.dispatchEvent(
            ot(rt.runStart, {
              lessonId: e.id
            })
          );
          const i = await t.compile({
            code: this.labState.code,
            lessonId: e.id,
            files: e.files
          }), s = Cc({
            lesson: e,
            code: this.labState.code,
            runResult: i
          });
          this.labState = $c(
            this.labState,
            s.runResult,
            s.summary
          ), this.dispatchEvent(
            ot(rt.runComplete, {
              lessonId: e.id,
              ok: s.runResult.ok,
              diagnostics: s.runResult.diagnostics,
              durationMs: s.runResult.durationMs
            })
          );
        } catch (t) {
          this.reportError(this.toFriendlyMessage(t, e.id), e.id);
        }
    };
  }
  disconnectedCallback() {
    this.editorAdapter?.dispose(), this.editorAdapter = void 0, this.disposeResolvedRunner(), super.disconnectedCallback();
  }
  willUpdate(e) {
    e.has("lesson") && this.lesson && this.applyPreparedLesson(this.lesson), e.has("showSolution") && this.activeLesson && (this.labState = {
      ...this.labState,
      solutionVisible: this.showSolution || this.labState.solutionVisible
    });
  }
  updated(e) {
    e.has("lesson") && !this.lesson && this.src && this.loadLesson(), e.has("lesson") && this.lesson && this.activeLesson && this.dispatchEvent(
      ot(rt.ready, {
        lessonId: this.activeLesson.id
      })
    ), e.has("src") && !this.lesson && this.src && this.loadLesson(), (e.has("runner") || e.has("runnerInstance") || e.has("ili2cJarUrl") || e.has("cheerpjLoaderUrl")) && this.disposeResolvedRunner(), this.syncEditorAdapter();
  }
  render() {
    const e = this.activeLesson;
    if (this.loadMessage)
      return Q`
        <section class="shell">
          <div class="section">
            <h2>INTERLIS Lab</h2>
            <p class="empty-state">${this.loadMessage}</p>
          </div>
        </section>
      `;
    if (!e)
      return Q`
        <section class="shell">
          <div class="section">
            <h2>INTERLIS Lab</h2>
            <p class="empty-state">Noch keine Lesson geladen.</p>
          </div>
        </section>
      `;
    const t = (e.hints ?? []).slice(0, this.labState.visibleHintCount), i = this.getStatusLabel(), s = this.getFeedbackMessage(), r = this.labState.lastResult;
    return Q`
      <section class="shell" data-theme=${this.theme}>
        <header class="lab-header">
          <div class="title-stack">
            <p class="eyebrow">INTERLIS Lab</p>
            <h2>${e.title}</h2>
            <div class="meta-row">
              ${e.level ? Q`<span>Niveau ${e.level}</span>` : V}
              <span>${e.type}</span>
              ${r?.durationMs ? Q`<span>${r.durationMs} ms</span>` : V}
            </div>
          </div>
          <div class="status-line" data-status=${this.labState.status} aria-live="polite">
            <span class="status-dot" aria-hidden="true"></span>
            <span>${i}</span>
          </div>
        </header>

        <div class="workbench">
          <aside class="lesson-panel" aria-label="Aufgabe">
            <p class="panel-label">Aufgabe</p>
            <h3>${e.task}</h3>
            <div class="lesson-context">
              <section>
                <h4>Ziel</h4>
                <p>${e.goal}</p>
              </section>
              ${e.explanation ? Q`
                    <section>
                      <h4>Kurz erklärt</h4>
                      <p>${e.explanation}</p>
                    </section>
                  ` : V}
            </div>
          </aside>

          <div class="workspace">
            <div class="editor-frame">
              <div class="editor-header">
                <span class="editor-title">INTERLIS-Code</span>
                <span>${this.readonly ? "Nur Lesen" : "Bearbeitbar"}</span>
              </div>
              <div class="editor-host">
                <textarea
                  id="interlis-editor"
                  class="native-editor"
                  .value=${this.labState.code}
                  ?readonly=${this.readonly}
                  spellcheck="false"
                  autocapitalize="off"
                  autocomplete="off"
                  aria-label="INTERLIS-Code Editor"
                  placeholder="INTERLIS 2.3; ..."
                  @input=${this.handleEditorInput}
                ></textarea>
              </div>
            </div>

            <div class="toolbar" role="toolbar" aria-label="Aktionen">
              <button class="primary" type="button" ?disabled=${this.labState.status === "running"} @click=${this.handleRun}>
                ${fe.run}
              </button>
              <button
                type="button"
                ?disabled=${(e.hints?.length ?? 0) === 0 || this.labState.visibleHintCount >= (e.hints?.length ?? 0)}
                @click=${this.handleHint}
              >
                ${this.labState.visibleHintCount > 0 ? fe.showNextHint : fe.showHint}
              </button>
              <button
                type="button"
                ?disabled=${!e.solution || this.labState.solutionVisible}
                @click=${this.handleSolution}
              >
                ${fe.showSolution}
              </button>
              <button type="button" ?disabled=${this.labState.status === "running"} @click=${this.handleReset}>
                ${fe.reset}
              </button>
            </div>

            <div class="feedback">
              <div class="feedback-summary" data-tone=${this.labState.status} aria-live="polite">
                <strong>${i}</strong>
                <p>${s}</p>
              </div>

              ${r?.diagnostics.length ? Q`
                    <ul class="diagnostics" aria-label="Diagnostik">
                      ${r.diagnostics.map(
      (o) => Q`
                          <li class=${`diagnostic-${o.severity}`}>
                            <span class="diagnostic-meta">
                              ${o.severity.toUpperCase()}${o.source ? Q` · ${o.source}` : V}${Ls(o) ? Q` · ${Ls(o).trim()}` : V}
                            </span>
                            <span>${o.message}</span>
                          </li>
                        `
    )}
                    </ul>
                  ` : V}

              ${this.renderRawOutput(r)}
            </div>
          </div>
        </div>

        ${t.length ? Q`
              <section class="support-section" aria-label="Hinweise">
                <div class="hint-list">
                  ${t.map((o, l) => this.renderHint(o, l, e.hints?.length ?? 0))}
                </div>
              </section>
            ` : V}

        ${this.labState.solutionVisible && e.solution ? Q`
              <section class="support-section" aria-label="Lösung und Vergleich">
                <div class="solution-grid">
                  <div class="solution-panel">
                    <h4>Musterlösung</h4>
                    <pre>${e.solution}</pre>
                  </div>
                  ${this.renderDiff(e.solution)}
                </div>
              </section>
            ` : V}

        ${e.reflection ? Q`
              <section class="support-section" aria-label="Reflexion">
                <div class="reflection-panel">
                  <h4>Reflexion</h4>
                  <p>${e.reflection.question}</p>
                </div>
              </section>
            ` : V}
      </section>
    `;
  }
  renderHint(e, t, i) {
    return Q`
      <article class="hint-item">
        <h4>${e.title ?? `Hinweis ${t + 1} von ${i}`}</h4>
        <p>${e.text}</p>
      </article>
    `;
  }
  renderRawOutput(e) {
    return !(e?.stdout || e?.stderr) ? V : Q`
      <details>
        <summary>${fe.rawOutput}</summary>
        ${e?.stdout ? Q`<pre>${e.stdout}</pre>` : V}
        ${e?.stderr ? Q`<pre>${e.stderr}</pre>` : V}
      </details>
    `;
  }
  renderDiff(e) {
    const i = Mc(this.labState.code, e).filter((s) => s.type !== "same");
    return i.length === 0 ? Q`
        <div class="solution-panel">
          <h4>${fe.compare}</h4>
          <p>Dein aktueller Code entspricht bereits der Musterlösung.</p>
        </div>
      ` : Q`
      <div class="solution-panel">
        <h4>${fe.compare}</h4>
        <table class="diff-table">
          <thead>
            <tr>
              <th>Zeile</th>
              <th>Dein Code</th>
              <th>Musterlösung</th>
            </tr>
          </thead>
          <tbody>
            ${i.map(
      (s) => Q`
                <tr class=${`diff-${s.type}`}>
                  <td>${s.lineNumber}</td>
                  <td>${s.current ?? ""}</td>
                  <td>${s.solution ?? ""}</td>
                </tr>
              `
    )}
          </tbody>
        </table>
      </div>
    `;
  }
  getStatusLabel() {
    switch (this.labState.status) {
      case "loading":
        return fe.statusLoading;
      case "ready":
        return fe.statusReady;
      case "running":
        return fe.statusRunning;
      case "success":
        return fe.statusSuccess;
      case "failed":
        return fe.statusFailed;
      case "error":
        return fe.statusError;
      default:
        return fe.statusIdle;
    }
  }
  getFeedbackMessage() {
    if (this.labState.error)
      return this.labState.error;
    if (this.labState.feedbackMessage)
      return this.labState.feedbackMessage;
    switch (this.labState.status) {
      case "loading":
        return "Die Lesson wird geladen und vorbereitet.";
      case "ready":
        return "Der Startcode ist bereit. Prüfe dein Modell, sobald du einen Zwischenstand testen möchtest.";
      case "running":
        return "Das Modell wird gerade kompiliert und gegen die Lesson-Checks geprüft.";
      case "success":
        return "Dein Modell erfüllt die aktuellen Prüfungen.";
      case "failed":
        return "Noch ist mindestens eine Prüfung offen. Die Diagnostik zeigt dir den nächsten sinnvollen Schritt.";
      case "idle":
        return "Lade eine Lesson, um mit dem Übungsfeld zu starten.";
      default:
        return "Es ist ein unerwarteter Fehler aufgetreten.";
    }
  }
  syncEditorAdapter() {
    this.textarea && (this.editorAdapter || (this.editorAdapter = new lm(this.textarea)), this.editorAdapter.setValue(this.labState.code), this.editorAdapter.setReadOnly?.(this.readonly), this.editorAdapter.setDiagnostics(this.labState.lastResult?.diagnostics ?? []));
  }
  async applyLesson(e) {
    try {
      const t = this.applyPreparedLesson(e);
      await this.updateComplete, this.dispatchEvent(
        ot(rt.ready, {
          lessonId: t.id
        })
      );
    } catch (t) {
      this.reportError(this.toFriendlyMessage(t), this.activeLesson?.id);
    }
  }
  async loadLesson() {
    if (!this.src)
      return;
    const e = ++this.lessonRequestToken;
    this.loadMessage = void 0, this.labState = Pc(this.labState);
    try {
      const t = await Bc(this.src);
      if (e !== this.lessonRequestToken)
        return;
      await this.applyLesson(t);
    } catch (t) {
      if (e !== this.lessonRequestToken)
        return;
      const i = this.toFriendlyMessage(t);
      this.activeLesson = void 0, this.loadMessage = `${i} Prüfe, ob die Datei ${this.src} existiert und gültiges JSON enthält.`, this.reportError(this.loadMessage);
    }
  }
  async getRunner() {
    const e = this.runnerInstance ? `instance:${this.runnerInstance.id}` : `named:${this.runner}:ili2c=${this.ili2cJarUrl ?? ""}:loader=${this.cheerpjLoaderUrl ?? ""}`;
    return this.resolvedRunner && this.resolvedRunnerKey === e ? this.resolvedRunner : (await this.disposeResolvedRunner(), this.resolvedRunner = this.runnerInstance ?? this.createRunnerFromName(this.runner), this.resolvedRunnerKey = e, this.resolvedRunner);
  }
  createRunnerFromName(e) {
    return e === "cheerpj" ? new Am({
      cheerpjLoaderUrl: this.cheerpjLoaderUrl,
      ili2cJarUrl: this.ili2cJarUrl
    }) : new cm();
  }
  async disposeResolvedRunner() {
    if (!this.resolvedRunner?.dispose) {
      this.resolvedRunner = void 0, this.resolvedRunnerKey = void 0;
      return;
    }
    await this.resolvedRunner.dispose(), this.resolvedRunner = void 0, this.resolvedRunnerKey = void 0;
  }
  reportError(e, t) {
    this.labState = Vc(this.labState, e), this.dispatchEvent(
      ot(rt.error, {
        lessonId: t,
        message: e
      })
    );
  }
  persistCode(e) {
    if (this.storageKey)
      try {
        localStorage.setItem(this.storageKey, e);
      } catch {
      }
  }
  readStoredCode() {
    if (this.storageKey)
      try {
        return localStorage.getItem(this.storageKey) ?? void 0;
      } catch {
        return;
      }
  }
  clearStoredCode() {
    if (this.storageKey)
      try {
        localStorage.removeItem(this.storageKey);
      } catch {
      }
  }
  toFriendlyMessage(e, t) {
    return e instanceof Error ? t ? `Fehler in Lesson ${t}: ${e.message}` : e.message : t ? `Fehler in Lesson ${t}.` : "Unbekannter Fehler.";
  }
  applyPreparedLesson(e) {
    const t = zl(e), i = this.readStoredCode() ?? t.initialCode;
    return this.activeLesson = t, this.loadMessage = void 0, this.labState = co(t, {
      code: i,
      showSolution: this.showSolution
    }), t;
  }
};
lo.styles = [Zm];
let oe = lo;
ve([
  Ve({ type: String })
], oe.prototype, "src");
ve([
  Ve({ type: Boolean })
], oe.prototype, "readonly");
ve([
  Ve({ type: Boolean, attribute: "show-solution" })
], oe.prototype, "showSolution");
ve([
  Ve({ type: String, attribute: "storage-key" })
], oe.prototype, "storageKey");
ve([
  Ve({ type: String })
], oe.prototype, "runner");
ve([
  Ve({ type: String, attribute: "ili2c-jar-url" })
], oe.prototype, "ili2cJarUrl");
ve([
  Ve({ type: String, attribute: "cheerpj-loader-url" })
], oe.prototype, "cheerpjLoaderUrl");
ve([
  Ve({ type: String })
], oe.prototype, "theme");
ve([
  Ve({ attribute: !1 })
], oe.prototype, "lesson");
ve([
  Ve({ attribute: !1 })
], oe.prototype, "runnerInstance");
ve([
  oo()
], oe.prototype, "activeLesson");
ve([
  oo()
], oe.prototype, "labState");
ve([
  oo()
], oe.prototype, "loadMessage");
ve([
  Xm("#interlis-editor")
], oe.prototype, "textarea");
customElements.get("interlis-lab") || customElements.define("interlis-lab", oe);
export {
  Am as CheerpJInterlisRunner,
  lm as CodeMirrorEditorAdapter,
  oe as InterlisLabElement,
  cm as MockInterlisRunner,
  Fc as TextareaEditorAdapter,
  Mc as buildInterlisDiff,
  $c as completeInterlisLabRun,
  ot as createInterlisLabEvent,
  co as createInterlisLabState,
  Pc as createLoadingState,
  fe as defaultLabels,
  Cc as evaluateInterlisLessonChecks,
  Vc as failInterlisLabState,
  Ls as formatDiagnosticLocation,
  n0 as formatDiagnosticMessage,
  t0 as getHighestDiagnosticSeverity,
  i0 as hasErrorDiagnostics,
  rt as interlisLabEvents,
  Bc as loadInterlisLabLesson,
  Sm as parseIli2cDiagnostics,
  zl as parseInterlisLabLesson,
  Hc as revealInterlisLabSolution,
  Nc as revealNextInterlisLabHint,
  Ic as startInterlisLabRun,
  s0 as updateInterlisLabCode
};
//# sourceMappingURL=interlis-lab.js.map
