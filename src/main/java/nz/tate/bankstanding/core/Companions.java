/*
 * Copyright (c) 2026, dan <dan@tate.nz>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nz.tate.bankstanding.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Collection log sets of pets that have kept you company while you stood, at any bank. A pet
 * is known by its follower's name, which covers every colour of it. Purely for the log.
 */
public final class Companions
{
	/** One kind of pet: any of its follower names counts, and the item is its icon. */
	public static final class Kind
	{
		public final String key;
		public final String name;
		public final int icon;
		final List<String> names;

		Kind(String key, String name, int icon, String... names)
		{
			this.key = key;
			this.name = name;
			this.icon = icon;
			this.names = Arrays.asList(names);
		}

		boolean is(String follower)
		{
			for (String each : names)
			{
				if (each.equalsIgnoreCase(follower))
				{
					return true;
				}
			}
			return false;
		}
	}

	public static final class Group
	{
		public final String key;
		public final String title;
		public final List<Kind> kinds;

		Group(String key, String title, Kind... kinds)
		{
			this.key = key;
			this.title = title;
			this.kinds = Collections.unmodifiableList(Arrays.asList(kinds));
		}

		/** The key saved in {@link Progress#moments} once this pet has stood with you. */
		public String momentKey(Kind kind)
		{
			return "pet:" + key + ":" + kind.key;
		}
	}

	public static final List<Group> GROUPS = Collections.unmodifiableList(Arrays.asList(
		new Group("cats", "Cats",
			new Kind("kitten", "Kitten", 1555, "Kitten"),
			new Kind("cat", "Cat", 1561, "Cat"),
			new Kind("overgrown-cat", "Overgrown cat", 1567, "Overgrown cat"),
			new Kind("lazy-cat", "Lazy cat", 6549, "Lazy cat"),
			new Kind("wily-cat", "Wily cat", 6555, "Wily cat"),
			new Kind("hellkitten", "Hellkitten", 7583, "Hellkitten"),
			new Kind("hellcat", "Hellcat", 7582, "Hellcat"),
			new Kind("overgrown-hellcat", "Overgrown hellcat", 7581, "Overgrown hellcat"),
			new Kind("lazy-hellcat", "Lazy hellcat", 7584, "Lazy hellcat"),
			new Kind("wily-hellcat", "Wily hellcat", 7585, "Wily hellcat")),
		new Group("dogs", "Dogs",
			dog("labrador", "Labrador", 34503),
			dog("husky", "Husky", 34509),
			dog("chihuahua", "Chihuahua", 34515),
			dog("border-collie", "Border collie", 34521),
			dog("corgi", "Corgi", 34527),
			dog("greyhound", "Greyhound", 34533),
			dog("pug", "Pug", 34539),
			dog("samoyed", "Samoyed", 34545),
			dog("bernese", "Bernese mountain dog", 34551),
			dog("shiba", "Shiba", 34557),
			dog("spaniel", "Spaniel", 34563),
			dog("yorkie", "Yorkie", 34569),
			new Kind("hellpuppy", "Hellpuppy", 13247, "Hellpuppy"))
	));

	private Companions()
	{
	}

	/** A breed counts as a puppy or grown up. The icon is the puppy. */
	private static Kind dog(String key, String breed, int icon)
	{
		return new Kind(key, breed, icon, breed, breed + " puppy");
	}

	/** Notes the pet following you; returns it if it's new to the log, else null. */
	public static Kind collect(Progress p, String follower, long now)
	{
		if (follower == null)
		{
			return null;
		}
		String name = follower.replace(' ', ' ').trim().toLowerCase(Locale.ENGLISH);
		for (Group group : GROUPS)
		{
			for (Kind kind : group.kinds)
			{
				if (kind.is(name) && Moments.record(p, group.momentKey(kind), now))
				{
					return kind;
				}
			}
		}
		return null;
	}
}
